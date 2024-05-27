package com.pkm.pinme.ui.scan

import android.Manifest.permission
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivityScanQrBinding
import com.pkm.pinme.factory.ViewModelFactory
import com.pkm.pinme.ui.main.MainActivity
import com.saadahmedev.popupdialog.PopupDialog
import com.pkm.pinme.utils.Result
import com.saadahmedev.popupdialog.listener.StandardDialogActionListener

class ScanQRActivity : AppCompatActivity() {

    // Binding
    private lateinit var activityScanQRBinding: ActivityScanQrBinding

    // Permissions
    @RequiresApi(VERSION_CODES.TIRAMISU)
    private val permissionAndroidTAbove = arrayOf(permission.CAMERA, permission.READ_MEDIA_VIDEO)
    private val permissionAndroidSBelow =
        arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    // ViewModel
    private lateinit var factory: ViewModelFactory
    private val viewModel: ScanQRViewModel by viewModels { factory }

    // Other var
    private var loadingDialog: PopupDialog? = null
    private lateinit var codeScanner: CodeScanner
    private var enableFlash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityScanQRBinding = ActivityScanQrBinding.inflate(layoutInflater)
        setContentView(activityScanQRBinding.root)
        setViewModelFactory()

        requestPermissions(getPermissionsForTargetSDK())
    }

    private fun requestFilter(filterId: String) {
        viewModel.getFilter(filterId).observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    setGrayBackground(true)
                    loadingDialog = PopupDialog.getInstance(this)
                        .progressDialogBuilder()
                        .createLottieDialog()
                        .setCancelable(false)
                        .setRawRes(R.raw.loading_bloc_anim)
                        .build()
                    loadingDialog?.show()
                }

                is Result.Error -> {
                    loadingDialog?.dismiss()
                        setGrayBackground(true)
                        PopupDialog.getInstance(this)
                            .statusDialogBuilder()
                            .createWarningDialog()
                            .setHeading("Oops...")
                            .setFontFamily(R.font.spartan_mb_semibold)
                            .setCancelable(false)
                            .setDescription(result.data)
                            .setActionButtonText("OK")
                            .build {
                                setGrayBackground(false)
                                codeScanner.startPreview()
                                it.dismiss()
                            }
                            .show();
                }

                is Result.Success -> {
                    loadingDialog?.dismiss()
                    val successDialog = PopupDialog.getInstance(this)
                        .standardDialogBuilder()
                        .createStandardDialog()
                        .setIcon(R.drawable.check_ic)
                        .setPositiveButtonBackgroundColor(R.color.primary)
                        .setFontFamily(R.font.spartan_mb_semibold)
                        .setPositiveButtonTextColor(R.color.white)
                        .setPositiveButtonCornerRadius(30.0f)
                        .setCancelable(false)
                        .setNegativeButtonCornerRadius(30.0f)
                        .setBackgroundCornerRadius(30.0f)
                        .setHeading(result.data.namaFilter ?: "Media Detected!")
                        .setDescription("Apakah Anda ingin melanjutkan untuk memindai AR?")
                        .setNegativeButtonText("Batal")
                        .setPositiveButtonText("Lanjut")
                        .build(object : StandardDialogActionListener {
                            override fun onPositiveButtonClicked(dialog: Dialog) {
                                setGrayBackground(false)

                                val intent = Intent(this@ScanQRActivity, MainActivity::class.java)
                                intent.putExtra("url", result.data.marker)

                                startActivity(intent)
                                dialog.dismiss()
                            }

                            override fun onNegativeButtonClicked(dialog: Dialog) {
                                setGrayBackground(false)
                                codeScanner.startPreview()
                                dialog.dismiss()
                            }
                        })
                        .show()
                }
            }
        }
    }

    private fun initiateScan() {
        codeScanner = CodeScanner(this, activityScanQRBinding.qrScannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false
        codeScanner.startPreview()
        activityScanQRBinding.flashBtn.setOnClickListener {
            enableFlash = !enableFlash
            if (enableFlash) {
                codeScanner.isFlashEnabled = true
                activityScanQRBinding.flashBtn.setBackgroundResource(R.drawable.flash_on_ic)
            } else {
                codeScanner.isFlashEnabled = false
                activityScanQRBinding.flashBtn.setBackgroundResource(R.drawable.flash_off_ic)
            }
        }
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                requestFilter(it.text)
            }
        }
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
        activityScanQRBinding.historyBtn.setOnClickListener {
            setGrayBackground(true)
            PopupDialog.getInstance(this)
                .statusDialogBuilder()
                .createWarningDialog()
                .setHeading("Coming Soon!")
                .setFontFamily(R.font.spartan_mb_semibold)
                .setCancelable(false)
                .setDescription("Fitur tersebut masih dalam pengembangan!")
                .setActionButtonText("OK")
                .build {
                    setGrayBackground(false)
                    it.dismiss()
                }
                .show();
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                Log.e("PERMISSIONS", "Granted")
            } else {
                Log.e("PERMISSIONS", "Not all permissions were granted")
            }
        }

        val permissionsNotGranted = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNotGranted.toTypedArray())
        } else {
            initiateScan()
        }
    }

    private fun getPermissionsForTargetSDK(): Array<String> =
        if (this.applicationInfo.targetSdkVersion >= VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
            permissionAndroidTAbove
        else
            permissionAndroidSBelow

    private fun setViewModelFactory() {
        factory = ViewModelFactory.getInstance(activityScanQRBinding.root.context)
    }

    private fun setGrayBackground(isGrayBackground: Boolean) {
        if (isGrayBackground) {
            activityScanQRBinding.root.setBackgroundColor(Color.GRAY)
        } else {
            activityScanQRBinding.root.setBackgroundColor(Color.TRANSPARENT)
        }

    }

}

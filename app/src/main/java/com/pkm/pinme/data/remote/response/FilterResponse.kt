package com.pkm.pinme.data.remote.response

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

@Parcelize
data class FilterResponse(

	@field:SerializedName("data")
	val data: FilterData,

	@field:SerializedName("error")
	val error: Boolean,

	@field:SerializedName("message")
	val message: String
) : Parcelable

@Parcelize
data class ArItem(

	@field:SerializedName("positionY")
	val positionY: Int? = null,

	@field:SerializedName("filterId")
	val filterId: String? = null,

	@field:SerializedName("ar")
	val ar: String? = null,

	@field:SerializedName("positionZ")
	val positionZ: Int? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("positionX")
	val positionX: Int? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
) : Parcelable

@Parcelize
data class FilterData(

	@field:SerializedName("preview")
	val preview: String? = null,

	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("ar")
	val ar: List<ArItem?>? = null,

	@field:SerializedName("clientId")
	val clientId: String? = null,

	@field:SerializedName("marker")
	val marker: String? = null,

	@field:SerializedName("sound")
	val sound: String? = null,

	@field:SerializedName("expiredDate")
	val expiredDate: String? = null,

	@field:SerializedName("namaFilter")
	val namaFilter: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("isActive")
	val isActive: Boolean? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
) : Parcelable

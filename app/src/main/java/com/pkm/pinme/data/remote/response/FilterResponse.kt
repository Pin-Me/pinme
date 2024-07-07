package com.pkm.pinme.data.remote.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterResponse(

    @field:SerializedName("data")
	val data: FilterModel,

    @field:SerializedName("error")
	val error: Boolean,

    @field:SerializedName("message")
	val message: String
) : Parcelable

@Parcelize
data class ArItem(

	@field:SerializedName("filterId")
	val filterId: String? = null,

	@field:SerializedName("ar")
	val ar: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("positionX")
	val positionX: Int? = null,

) : Parcelable

@Parcelize
data class FilterModel(

	@field:SerializedName("preview")
	val preview: String? = null,

	@field:SerializedName("ar")
	val ar: List<ArItem?>? = null,

	@field:SerializedName("clientId")
	val clientId: String? = null,

	@field:SerializedName("marker")
	val marker: String? = null,

	@field:SerializedName("sound")
	val sound: String? = null,

	@field:SerializedName("namaFilter")
	val namaFilter: String? = null,

	@field:SerializedName("id")
	val id: String? = null,
) : Parcelable

package me.rhunk.snapenhance.common.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class ThemeColorEntry(
    @SerializedName("key")
    val key: String,
    @SerializedName("value")
    var value: Int,
): Parcelable

@Parcelize
data class DatabaseThemeContent(
    @SerializedName("colors")
    val colors: List<ThemeColorEntry> = emptyList(),
): Parcelable

data class DatabaseTheme(
    val id: Int,
    val enabled: Boolean,
    val name: String,
    val version: String?,
    val author: String?,
    val updateUrl: String?,
)

data class ExportedTheme(
    val name: String,
    val version: String?,
    val author: String?,
    val content: DatabaseThemeContent,
)

enum class ThemingAttribute {
    COLOR
}

val AvailableThemingAttributes = mapOf(
    ThemingAttribute.COLOR to listOf(
        "sigColorTextPrimary",
        "sigColorBackgroundSurface",
        "sigColorBackgroundMain",
        "actionSheetBackgroundDrawable",
        "actionSheetRoundedBackgroundDrawable",
        "sigColorChatChat",
        "sigColorChatPendingSending",
        "sigColorChatSnapWithSound",
        "sigColorChatSnapWithoutSound",
        "sigExceptionColorCameraGridLines",
        "listDivider",
        "listBackgroundDrawable",
        "sigColorIconPrimary",
        "actionSheetDescriptionTextColor",
        "ringColor",
        "sigColorIconSecondary",
        "itemShapeFillColor",
        "ringStartColor",
        "sigColorLayoutPlaceholder",
        "scButtonColor",
        "recipientPillBackgroundDrawable",
        "boxBackgroundColor",
        "editTextColor",
        "chipBackgroundColor",
        "recipientInputStyle",
        "rangeFillColor",
        "pstsIndicatorColor",
        "pstsTabBackground",
        "pstsDividerColor",
        "tabTextColor",
        "statusBarForeground",
        "statusBarBackground",
        "strokeColor",
        "storyReplayViewRingColor",
        "sigColorButtonPrimary",
        "sigColorBaseAppYellow",
        "sigColorBackgroundSurfaceTranslucent",
        "sigColorStoryRingFriendsFeedStoryRing",
        "sigColorStoryRingDiscoverTabThumbnailStoryRing",
    )
)

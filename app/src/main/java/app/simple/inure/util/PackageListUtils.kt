package app.simple.inure.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import app.simple.inure.R
import app.simple.inure.apk.utils.MetaUtils
import app.simple.inure.apk.utils.PackageUtils.getApplicationInstallTime
import app.simple.inure.apk.utils.PackageUtils.getPackageSize
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.preferences.DevelopmentPreferences
import app.simple.inure.preferences.FormattingPreferences
import app.simple.inure.util.DateUtils.toDate
import app.simple.inure.util.FileSizeHelper.getDirectorySize
import app.simple.inure.util.FileSizeHelper.toLength
import app.simple.inure.util.FileSizeHelper.toSize

object PackageListUtils {
    fun TypeFaceTextView.setAppInfo(packageInfo: PackageInfo) {
        buildString {
            //            append(packageInfo.versionName)
            //            append(" | ")

            if ((packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                append(context.getString(R.string.user))
            } else {
                append(context.getString(R.string.system))
            }

            append(" | ")
            if (DevelopmentPreferences.get(DevelopmentPreferences.showCompleteAppSize)) {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                    }
                } else {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength() +
                                packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize()).toSize())
                    }
                }
            } else {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    append(packageInfo.applicationInfo.sourceDir.toSize())
                } else {
                    append((packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize() +
                            packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                }
            }
            append(" | ")

            if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0) {
                append(context.getString(R.string.uninstalled))
            } else {
                if (packageInfo.applicationInfo.enabled) {
                    append(context.getString(R.string.enabled))
                } else {
                    append(context.getString(R.string.disabled))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                append(" | ")
                append(MetaUtils.getCategory(packageInfo.applicationInfo.category, context))
            }

            append(" | ")

            if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                append(context.getString(R.string.apk))
            } else {
                append(context.getString(R.string.split_packages))
            }

            append(" | ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                append(packageInfo.applicationInfo.minSdkVersion)
                append("..")
                append(packageInfo.applicationInfo.targetSdkVersion)
            } else {
                append(packageInfo.applicationInfo.targetSdkVersion)
            }

            text = toString()
        }
    }

    fun TypeFaceTextView.setUninstalledAppInfo(packageInfo: PackageInfo) {
        buildString {
            append(packageInfo.versionName)
            append(" | ")

            if ((packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                append(context.getString(R.string.user))
            } else {
                append(context.getString(R.string.system))
            }

            append(" | ")
            if (DevelopmentPreferences.get(DevelopmentPreferences.showCompleteAppSize)) {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                    }
                } else {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength() +
                                packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize()).toSize())
                    }
                }
            } else {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    append(packageInfo.applicationInfo.sourceDir.toSize())
                } else {
                    append((packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize() +
                            packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                append(" | ")
                append(MetaUtils.getCategory(packageInfo.applicationInfo.category, context))
            }

            if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                append(" | ")
                append(context.getString(R.string.apk))
            } else {
                append(" | ")
                append(context.getString(R.string.split_packages))
            }

            append(" | ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                append(packageInfo.applicationInfo.minSdkVersion)
                append("..")
                append(packageInfo.applicationInfo.targetSdkVersion)
            } else {
                append(packageInfo.applicationInfo.targetSdkVersion)
            }

            text = toString()
        }
    }

    fun TypeFaceTextView.setRecentlyInstalledInfo(packageInfo: PackageInfo) {
        buildString {
            append(packageInfo.getApplicationInstallTime(context, FormattingPreferences.getDateFormat()))
            append(" | ")
            //            append(packageInfo.versionName)
            //            append(" | ")
            if (DevelopmentPreferences.get(DevelopmentPreferences.showCompleteAppSize)) {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                    }
                } else {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength() +
                                packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize()).toSize())
                    }
                }
            } else {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    append(packageInfo.applicationInfo.sourceDir.toSize())
                } else {
                    append((packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize() +
                            packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                }
            }
            append(" | ")
            if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                append(context.getString(R.string.apk))
            } else {
                append(context.getString(R.string.split_packages))
            }

            append(" | ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                append(packageInfo.applicationInfo.minSdkVersion)
                append("..")
                append(packageInfo.applicationInfo.targetSdkVersion)
            } else {
                append(packageInfo.applicationInfo.targetSdkVersion)
            }

        }.also { text = it }
    }

    fun TypeFaceTextView.setRecentlyUpdatedInfo(packageInfo: PackageInfo) {
        buildString {
            append(packageInfo.lastUpdateTime.toDate())
            append(" | ")
            //            append(packageInfo.versionName)
            //            append(" | ")
            if (DevelopmentPreferences.get(DevelopmentPreferences.showCompleteAppSize)) {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                    }
                } else {
                    with(packageInfo.getPackageSize(context)) {
                        append((cacheSize +
                                dataSize +
                                codeSize +
                                externalCacheSize +
                                externalDataSize +
                                externalMediaSize +
                                externalObbSize +
                                externalCodeSize +
                                packageInfo.applicationInfo.sourceDir.toLength() +
                                packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize()).toSize())
                    }
                }
            } else {
                if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                    append(packageInfo.applicationInfo.sourceDir.toSize())
                } else {
                    append((packageInfo.applicationInfo.splitSourceDirs!!.getDirectorySize() +
                            packageInfo.applicationInfo.sourceDir.toLength()).toSize())
                }
            }
            append(" | ")
            if (packageInfo.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                append(context.getString(R.string.apk))
            } else {
                append(context.getString(R.string.split_packages))
            }

            append(" | ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                append(packageInfo.applicationInfo.minSdkVersion)
                append("..")
                append(packageInfo.applicationInfo.targetSdkVersion)
            } else {
                append(packageInfo.applicationInfo.targetSdkVersion)
            }

        }.also { text = it }
    }
}
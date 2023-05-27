package app.simple.inure.viewmodels.panels

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.apk.parsers.APKParser
import app.simple.inure.extensions.viewmodels.PackageUtilsViewModel
import app.simple.inure.models.SearchModel
import app.simple.inure.popups.apps.PopupAppsCategory
import app.simple.inure.preferences.SearchPreferences
import app.simple.inure.util.Sort.getSortedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.stream.Collectors

class SearchViewModel(application: Application) : PackageUtilsViewModel(application) {

    private val searchKeywords: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            it.postValue(SearchPreferences.getLastSearchKeyword())
        }
    }

    private val searchData: MutableLiveData<ArrayList<PackageInfo>> by lazy {
        MutableLiveData<ArrayList<PackageInfo>>().also {
            loadPackageData()
        }
    }

    private val deepSearchData: MutableLiveData<ArrayList<SearchModel>> by lazy {
        MutableLiveData<ArrayList<SearchModel>>().also {
            loadPackageData()
        }
    }

    fun getSearchKeywords(): LiveData<String> {
        return searchKeywords
    }

    fun setSearchKeywords(keywords: String) {
        SearchPreferences.setLastSearchKeyword(keywords)
        searchKeywords.postValue(keywords)
        initiateSearch(keywords)
    }

    fun getSearchData(): LiveData<ArrayList<PackageInfo>> {
        return searchData
    }

    fun getDeepSearchData(): LiveData<ArrayList<SearchModel>> {
        return deepSearchData
    }

    fun initiateSearch(keywords: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (SearchPreferences.isDeepSearchEnabled()) {
                loadDeepSearchData(keywords)
            } else {
                loadSearchData(keywords)
            }
        }
    }

    private fun loadSearchData(keywords: String) {
        var apps = getInstalledApps()

        if (keywords.isEmpty()) {
            searchData.postValue(arrayListOf())
            return
        }

        apps = apps.stream().filter { p ->
            p.applicationInfo.name.contains(keywords, SearchPreferences.isCasingIgnored())
                    || p.packageName.contains(keywords, SearchPreferences.isCasingIgnored())
        }.collect(Collectors.toList()) as ArrayList<PackageInfo>

        when (SearchPreferences.getAppsCategory()) {
            PopupAppsCategory.SYSTEM -> {
                apps = apps.stream().filter { p ->
                    p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                }.collect(Collectors.toList()) as ArrayList<PackageInfo>
            }
            PopupAppsCategory.USER -> {
                apps = apps.stream().filter { p ->
                    p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                }.collect(Collectors.toList()) as ArrayList<PackageInfo>
            }
        }

        apps.getSortedList(SearchPreferences.getSortStyle(), SearchPreferences.isReverseSorting())

        searchData.postValue(apps)
    }

    private fun loadDeepSearchData(keywords: String) {
        var list = arrayListOf<SearchModel>()
        var apps = getInstalledApps()

        if (keywords.isEmpty()) {
            deepSearchData.postValue(arrayListOf())
            return
        }

        when (SearchPreferences.getAppsCategory()) {
            PopupAppsCategory.SYSTEM -> {
                apps = apps.stream().filter { p ->
                    p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                }.collect(Collectors.toList()) as ArrayList<PackageInfo>
            }
            PopupAppsCategory.USER -> {
                apps = apps.stream().filter { p ->
                    p.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                }.collect(Collectors.toList()) as ArrayList<PackageInfo>
            }
        }

        apps.getSortedList(SearchPreferences.getSortStyle(), SearchPreferences.isReverseSorting())

        for (app in apps) {
            val searchModel = SearchModel()

            searchModel.packageInfo = app
            searchModel.permissions = getPermissionCount(keywords, app)
            searchModel.activities = getActivitiesCount(keywords, app)
            searchModel.services = getServicesCount(keywords, app)
            searchModel.receivers = getReceiversCount(keywords, app)
            searchModel.providers = getProvidersCount(keywords, app)
            searchModel.resources = getResourcesCount(keywords, app)

            list.add(searchModel)
        }

        list = list.filter {
            it.permissions > 0 || it.activities > 0 || it.services > 0 || it.receivers > 0 || it.providers > 0 || it.resources > 0 ||
                    it.packageInfo.applicationInfo.name.contains(keywords, SearchPreferences.isCasingIgnored()) ||
                    it.packageInfo.packageName.contains(keywords, SearchPreferences.isCasingIgnored())
        } as ArrayList<SearchModel>

        deepSearchData.postValue(list)
    }

    private fun getPermissionCount(keyword: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            if (app.requestedPermissions != null) {
                Log.d("Deep Search", "Permissions: ${app.applicationInfo.name} : ${app.requestedPermissions.size}")
                for (permission in app.requestedPermissions) {
                    if (permission.lowercase().contains(keyword.lowercase())) {
                        // Log.d("Deep Search", "$keyword : $permission")
                        count = count.inc()
                    }
                }
            } else {
                Log.d("Deep Search", "Permissions: ${app.applicationInfo.name} : 0")
            }
        }

        return count
    }

    private fun getActivitiesCount(keywords: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            if (app.activities != null) {
                Log.d("Deep Search", "Activities: ${app.applicationInfo.name} : ${app.activities.size}")
                for (i in app.activities) {
                    if (i.name.lowercase().contains(keywords.lowercase())) {
                        count = count.inc()
                    }
                }
            }
        }

        return count
    }

    private fun getServicesCount(keywords: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            if (app.services != null) {
                Log.d("Deep Search", "Services: ${app.applicationInfo.name} : ${app.services.size}")
                for (i in app.services) {
                    if (i.name.lowercase().contains(keywords.lowercase())) {
                        count = count.inc()
                    }
                }
            }
        }

        return count
    }

    private fun getReceiversCount(keywords: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            if (app.receivers != null) {
                Log.d("Deep Search", "Receivers: ${app.applicationInfo.name} : ${app.receivers.size}")
                for (i in app.receivers) {
                    if (i.name.lowercase().contains(keywords.lowercase())) {
                        count = count.inc()
                    }
                }
            }
        }

        return count
    }

    private fun getProvidersCount(keywords: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            if (app.providers != null) {
                Log.d("Deep Search", "Providers: ${app.applicationInfo.name} : ${app.providers.size}")
                for (i in app.providers) {
                    if (i.name.lowercase().contains(keywords.lowercase())) {
                        count = count.inc()
                    }
                }
            }
        }

        return count
    }

    private fun getResourcesCount(keywords: String, app: PackageInfo): Int {
        var count = 0

        kotlin.runCatching {
            with(APKParser.getXmlFiles(app.applicationInfo.sourceDir, keywords)) {
                count = count()
            }
        }

        return count
    }

    override fun onAppsLoaded(apps: ArrayList<PackageInfo>) {
        initiateSearch(SearchPreferences.getLastSearchKeyword())
    }

    override fun onAppUninstalled(packageName: String?) {
        super.onAppUninstalled(packageName)
        initiateSearch(SearchPreferences.getLastSearchKeyword())
    }
}
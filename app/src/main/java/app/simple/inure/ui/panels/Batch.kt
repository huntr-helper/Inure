package app.simple.inure.ui.panels

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.ui.AdapterBatch
import app.simple.inure.constants.BottomMenuConstants
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.app.Sure.Companion.newSureInstance
import app.simple.inure.dialogs.batch.BatchBatteryOptimization.Companion.showBatchBatteryOptimization
import app.simple.inure.dialogs.batch.BatchExtract.Companion.showBatchExtract
import app.simple.inure.dialogs.batch.BatchForceStop.Companion.showBatchForceStop
import app.simple.inure.dialogs.batch.BatchMenu
import app.simple.inure.dialogs.batch.BatchSort.Companion.showBatchSort
import app.simple.inure.dialogs.batch.BatchState.Companion.showBatchStateDialog
import app.simple.inure.dialogs.batch.BatchUninstaller
import app.simple.inure.dialogs.menus.AppsMenu
import app.simple.inure.dialogs.miscellaneous.GenerateAppData.Companion.showGeneratedDataTypeSelector
import app.simple.inure.dialogs.miscellaneous.StoragePermission
import app.simple.inure.dialogs.miscellaneous.StoragePermission.Companion.showStoragePermissionDialog
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AdapterCallbacks
import app.simple.inure.interfaces.fragments.SureCallbacks
import app.simple.inure.models.BatchPackageInfo
import app.simple.inure.popups.batch.PopupBatchState
import app.simple.inure.preferences.BatchPreferences
import app.simple.inure.ui.subpanels.BatchSelectedApps
import app.simple.inure.ui.viewers.HtmlViewer
import app.simple.inure.ui.viewers.JSON
import app.simple.inure.ui.viewers.Markdown
import app.simple.inure.ui.viewers.XMLViewerTextView
import app.simple.inure.util.NullSafety.isNotNull
import app.simple.inure.util.PermissionUtils.checkStoragePermission
import app.simple.inure.viewmodels.panels.BatchViewModel

class Batch : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView

    private var adapterBatch: AdapterBatch? = null
    private lateinit var batchViewModel: BatchViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_batch, container, false)

        recyclerView = view.findViewById(R.id.batch_recycler_view)
        batchViewModel = ViewModelProvider(requireActivity())[BatchViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullVersionCheck()
        showLoader()

        batchViewModel.getBatchData().observe(viewLifecycleOwner) {
            adapterBatch = AdapterBatch(it)

            hideLoader()

            adapterBatch?.setOnItemClickListener(object : AdapterCallbacks {
                override fun onAppClicked(packageInfo: PackageInfo, icon: ImageView) {
                    openFragmentArc(AppInfo.newInstance(packageInfo), icon, "app_info")
                }

                override fun onAppLongPressed(packageInfo: PackageInfo, icon: ImageView) {
                    AppsMenu.newInstance(packageInfo)
                        .show(childFragmentManager, "apps_menu")
                }

                override fun onBatchChanged(batchPackageInfo: BatchPackageInfo) {
                    batchViewModel.updateBatchItem(batchPackageInfo, updateSelected = true)
                    setupBottomMenu()
                }
            })

            recyclerView.adapter = adapterBatch
            setupBottomMenu()

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }
    }

    private fun setupBottomMenu() {
        if (bottomRightCornerMenu?.menuAdapter?.itemCount == getBatchMenuItems().size) {
            return
        } else {
            bottomRightCornerMenu?.setInitialized(false)
        }

        bottomRightCornerMenu?.initBottomMenuWithRecyclerView(getBatchMenuItems(), recyclerView) { id, view ->
            when (id) {
                R.drawable.ic_select_all -> {
                    showLoader(manualOverride = true)

                    if (adapterBatch?.isAllSelected() == true) {
                        batchViewModel.deselectAllBatchItems()
                    } else {
                        batchViewModel.selectAllBatchItems()
                    }
                }

                R.drawable.ic_filter -> {
                    childFragmentManager.showBatchSort()
                }

                R.drawable.ic_search -> {
                    openFragmentSlide(Search.newInstance(true), "search")
                }

                R.drawable.ic_settings -> {
                    BatchMenu.newInstance()
                        .show(childFragmentManager, "batch_menu")
                }

                R.drawable.ic_delete -> {
                    if (adapterBatch?.getSelectedAppsCount()!! < adapterBatch?.itemCount!!.minus(1)) { // We're subtracting one because header
                        childFragmentManager.newSureInstance().setOnSureCallbackListener(object : SureCallbacks {
                            override fun onSure() {
                                BatchUninstaller.newInstance(adapterBatch!!.getCurrentAppsList())
                                    .show(childFragmentManager, "batch_uninstaller")
                            }
                        })
                    } else {
                        showWarning("RESTRICTION: Cannot uninstall all apps at once", goBack = false)
                    }
                }

                R.drawable.ic_hide_source -> {
                    if (adapterBatch?.getSelectedAppsCount()!! < adapterBatch?.itemCount!!.minus(1)) { // We're subtracting one because header
                        PopupBatchState(view).setOnPopupBatchStateCallbacks(object : PopupBatchState.Companion.PopupBatchStateCallbacks {
                            override fun onEnableAll() {
                                onSure {
                                    childFragmentManager.showBatchStateDialog(adapterBatch!!.getCurrentAppsList(), true) {
                                        for (app in adapterBatch!!.getCurrentAppsList()) {
                                            adapterBatch!!.updateBatchItem(app)
                                        }
                                    }
                                }
                            }

                            override fun onDisableAll() {
                                onSure {
                                    childFragmentManager.showBatchStateDialog(adapterBatch!!.getCurrentAppsList(), false) {
                                        for (app in adapterBatch!!.getCurrentAppsList()) {
                                            adapterBatch!!.updateBatchItem(app)
                                        }
                                    }
                                }
                            }
                        })
                    } else {
                        showWarning("RESTRICTION: Cannot change state of all apps at once", goBack = false)
                    }
                }

                R.drawable.ic_send -> {
                    /* no-op */
                }

                R.drawable.ic_downloading -> {
                    childFragmentManager.newSureInstance().setOnSureCallbackListener(object : SureCallbacks {
                        override fun onSure() {
                            if (requireContext().checkStoragePermission()) {
                                childFragmentManager.showBatchExtract(adapterBatch?.getCurrentAppsList()!!)
                            } else {
                                childFragmentManager.showStoragePermissionDialog().setStoragePermissionCallbacks(
                                        object : StoragePermission.Companion.StoragePermissionCallbacks {
                                            override fun onStoragePermissionGranted() {
                                                childFragmentManager.showBatchExtract(adapterBatch?.getCurrentAppsList()!!)
                                            }
                                        })
                            }
                        }
                    })
                }

                R.drawable.ic_text_snippet -> {
                    childFragmentManager.showGeneratedDataTypeSelector().onGenerateData {
                        showLoader(manualOverride = true)
                        adapterBatch?.getCurrentAppsList()?.let {
                            batchViewModel.generateAppsData(it)
                            Log.d("Batch", "Generating data for ${it.size} apps")
                        }
                    }
                }

                R.drawable.ic_settings_power -> {
                    childFragmentManager.showBatchBatteryOptimization(adapterBatch?.getCurrentAppsList()!!)
                }

                R.drawable.ic_checklist -> {
                    openFragmentSlide(BatchSelectedApps.newInstance(), "batch_selected_apps")
                }

                R.drawable.ic_refresh -> {
                    showLoader(manualOverride = true)
                    batchViewModel.refreshPackageData()
                }

                R.drawable.ic_close -> {
                    onSure {
                        childFragmentManager.showBatchForceStop(adapterBatch?.getCurrentAppsList()!!)
                    }
                }
            }
        }

        batchViewModel.getGeneratedDataPath().observe(viewLifecycleOwner) {
            if (it.isNotNull()) {
                Log.d("Batch", "Generated data path: $it")
                hideLoader()
                when {
                    it.endsWith(".xml") ||
                            it.endsWith(".txt") ||
                            it.endsWith(".csv") -> {
                        openFragmentSlide(
                                XMLViewerTextView
                                    .newInstance(packageInfo = PackageInfo(), /* Empty package info */
                                                 isManifest = false,
                                                 pathToXml = it,
                                                 isRaw = true), "xml_viewer")
                    }

                    it.endsWith(".html") -> {
                        openFragmentSlide(
                                HtmlViewer
                                    .newInstance(packageInfo = PackageInfo(), it,
                                                 isRaw = true), "web_page")
                    }

                    it.endsWith(".json") -> {
                        openFragmentSlide(
                                JSON.newInstance(packageInfo = PackageInfo(),
                                                 path = it,
                                                 isRaw = true), "json_viewer")
                    }

                    it.endsWith(".md") -> {
                        openFragmentSlide(
                                Markdown.newInstance(packageInfo = PackageInfo(),
                                                     path = it,
                                                     isRaw = true), "markdown_viewer")
                    }
                }

                batchViewModel.clearGeneratedAppsDataLiveData()
            } else {
                hideLoader()
            }
        }
    }

    private fun getBatchMenuItems(): ArrayList<Pair<Int, Int>> {
        for (batch in adapterBatch?.getCurrentAppsList()!!) {
            if (batch.isSelected) {
                return BottomMenuConstants.getBatchMenu()
            }
        }

        return BottomMenuConstants.getBatchUnselectedMenu()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            BatchPreferences.moveSelectionTop -> {
                adapterBatch?.moveSelectedItemsToTheTop()
            }

            BatchPreferences.highlightSelected -> {
                adapterBatch?.updateSelectionsHighlights(BatchPreferences.isSelectedBatchHighlighted())
            }

            BatchPreferences.isSortingReversed,
            BatchPreferences.listAppsCategory,
            BatchPreferences.sortStyle,
            BatchPreferences.listAppsFilter -> {
                batchViewModel.refresh()
            }
        }
    }

    companion object {
        fun newInstance(loading: Boolean = false): Batch {
            val args = Bundle()
            val fragment = Batch()
            args.putBoolean(BundleConstants.loading, loading)
            fragment.arguments = args
            return fragment
        }
    }
}
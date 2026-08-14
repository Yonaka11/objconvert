package com.starbygigi.pricescan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.starbygigi.pricescan.databinding.ActivityMainBinding
import com.starbygigi.pricescan.databinding.DialogPriceEntryBinding
import com.starbygigi.pricescan.databinding.DialogSettingsBinding
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPrefs
    private lateinit var api: SheetApiClient
    private lateinit var cameraExecutor: ExecutorService

    /** Guards against the analyzer firing again while a dialog for the current scan is open. */
    private val isBusy = AtomicBoolean(false)

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else setStatus(getString(R.string.camera_permission_required))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPrefs(this)
        api = SheetApiClient(prefs)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                showSettingsDialog()
                true
            } else {
                false
            }
        }

        binding.manualEntryButton.setOnClickListener { showManualEntryDialog() }

        if (!prefs.isConfigured) {
            showSettingsDialog(forcedFirstRun = true)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode -> onBarcodeDetected(barcode) })
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (exc: Exception) {
                setStatus(getString(R.string.camera_start_failed, exc.message))
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onBarcodeDetected(barcode: String) {
        if (isBusy.getAndSet(true)) return
        runOnUiThread {
            setStatus(getString(R.string.looking_up, barcode))
            lookup(barcode)
        }
    }

    private fun lookup(barcode: String) {
        lifecycleScope.launch {
            try {
                val result = api.lookup(barcode)
                if (result.found) showExistingItemDialog(result) else showNewItemDialog(barcode)
            } catch (e: Exception) {
                toastError(e)
                resumeScanning()
            }
        }
    }

    private fun showNewItemDialog(barcode: String) {
        val dialogBinding = DialogPriceEntryBinding.inflate(layoutInflater)
        dialogBinding.nameLayout.isVisible = true

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.new_item_title, barcode))
            .setMessage(R.string.new_item_message)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.nameInput.text?.toString().orEmpty().trim()
                val price = dialogBinding.priceInput.text?.toString()?.toDoubleOrNull()
                if (price == null) {
                    Toast.makeText(this, R.string.invalid_price, Toast.LENGTH_SHORT).show()
                    resumeScanning()
                } else {
                    createItem(barcode, name, price)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> resumeScanning() }
            .setOnCancelListener { resumeScanning() }
            .show()
    }

    private fun createItem(barcode: String, name: String, price: Double) {
        setStatus(getString(R.string.saving))
        lifecycleScope.launch {
            try {
                api.createItem(barcode, name, price)
                Toast.makeText(this@MainActivity, getString(R.string.saved_new, barcode), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                toastError(e)
            } finally {
                resumeScanning()
            }
        }
    }

    private fun showExistingItemDialog(result: LookupResult) {
        val label = result.name.ifBlank { result.barcode }
        AlertDialog.Builder(this)
            .setTitle(R.string.item_exists_title)
            .setMessage(getString(R.string.item_exists_message, label, formatPrice(result.price)))
            .setPositiveButton(R.string.update_price) { _, _ -> showUpdatePriceDialog(result) }
            .setNegativeButton(R.string.no_thanks) { _, _ -> resumeScanning() }
            .setOnCancelListener { resumeScanning() }
            .show()
    }

    private fun showUpdatePriceDialog(result: LookupResult) {
        val dialogBinding = DialogPriceEntryBinding.inflate(layoutInflater)
        dialogBinding.nameLayout.isVisible = false
        dialogBinding.priceInput.setText(result.price?.let { formatPriceForInput(it) } ?: "")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_price_title, result.barcode))
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.update) { _, _ ->
                val price = dialogBinding.priceInput.text?.toString()?.toDoubleOrNull()
                if (price == null) {
                    Toast.makeText(this, R.string.invalid_price, Toast.LENGTH_SHORT).show()
                    resumeScanning()
                } else {
                    updatePrice(result.barcode, price)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> resumeScanning() }
            .setOnCancelListener { resumeScanning() }
            .show()
    }

    private fun updatePrice(barcode: String, price: Double) {
        setStatus(getString(R.string.saving))
        lifecycleScope.launch {
            try {
                api.updateItem(barcode, price)
                Toast.makeText(this@MainActivity, getString(R.string.price_updated, barcode), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                toastError(e)
            } finally {
                resumeScanning()
            }
        }
    }

    private fun showManualEntryDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.manual_entry_hint)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.manual_entry_title)
            .setView(input)
            .setPositiveButton(R.string.lookup_button) { _, _ ->
                val barcode = input.text?.toString()?.trim().orEmpty()
                if (barcode.isNotEmpty()) onBarcodeDetected(barcode)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSettingsDialog(forcedFirstRun: Boolean = false) {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
        dialogBinding.urlInput.setText(prefs.webAppUrl.orEmpty())
        dialogBinding.tokenInput.setText(prefs.token)

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                prefs.webAppUrl = dialogBinding.urlInput.text?.toString().orEmpty()
                prefs.token = dialogBinding.tokenInput.text?.toString().orEmpty()
                Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            }

        if (forcedFirstRun) {
            builder.setMessage(R.string.settings_first_run_message)
        } else {
            builder.setNegativeButton(R.string.cancel, null)
        }

        builder.show()
    }

    private fun resumeScanning() {
        isBusy.set(false)
        setStatus(getString(R.string.point_camera_hint))
    }

    private fun setStatus(text: String) {
        binding.statusText.text = text
    }

    private fun toastError(e: Exception) {
        Toast.makeText(this, e.message ?: getString(R.string.generic_error), Toast.LENGTH_LONG).show()
    }

    private fun formatPrice(price: Double?): String =
        price?.let { String.format(Locale.US, "$%.2f", it) } ?: getString(R.string.no_price_on_file)

    private fun formatPriceForInput(price: Double): String =
        if (price == price.toLong().toDouble()) price.toLong().toString() else price.toString()
}

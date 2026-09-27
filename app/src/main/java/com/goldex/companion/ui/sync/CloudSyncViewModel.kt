package com.goldex.companion.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.sync.*
import com.goldex.companion.model.PersianNumberFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class CloudFormState(val challenge: JSONObject? = null, val requestedAtMillis: Long = 0, val phone: String = "", val busy: Boolean = false, val error: String = "", val review: JSONObject? = null, val devices: JSONObject? = null, val backupPath: String? = null)
class CloudSyncViewModel(application: Application) : AndroidViewModel(application) {
    val coordinator=SyncCoordinator.get(application)
    val state=coordinator.state
    private val _form=MutableStateFlow(CloudFormState())
    val form=_form.asStateFlow()
    private fun action(block: suspend () -> Unit) {
        if(_form.value.busy) return
        _form.update { it.copy(busy=true,error="") }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { _form.update { it.copy(error=SyncCoordinator.message((e as? CloudException)?.code ?: "SERVER_ERROR")) } }
            finally { _form.update { it.copy(busy=false) } }
        }
    }
    fun setEnabled(value: Boolean)=coordinator.setEnabled(value)
    fun requestCode(phone: String)=action {
        val clean=PersianNumberFormatter.toEnglishDigits(phone).trim()
        val challenge=coordinator.requestCode(clean)
        _form.update { it.copy(challenge=challenge,phone=clean,requestedAtMillis=System.currentTimeMillis()) }
    }
    fun verifyCode(code: String)=action {
        val form=_form.value
        coordinator.verifyCode(requireNotNull(form.challenge),PersianNumberFormatter.toEnglishDigits(code),form.phone)
        _form.update { it.copy(challenge=null) }
    }
    fun changePhone() { if (!_form.value.busy) _form.update { it.copy(challenge=null,error="") } }
    fun sync()=action { coordinator.sync() }
    fun activateTrial()=action { coordinator.activateTrial() }
    fun restore()=action { coordinator.restore(true) }
    fun reauthenticate()=coordinator.reauthenticate()
    fun takeover()=action { coordinator.takeover(true) }
    fun reviewConflict()=action { val review=coordinator.reviewConflict(); _form.update { it.copy(review=review) } }
    fun closeReview() { _form.update { it.copy(review=null,devices=null) } }
    fun chooseLocal()=action { coordinator.chooseLocalConflict(requireNotNull(_form.value.review).getString("operationId")); closeReview() }
    fun exportBackup()=action { val file=coordinator.backupForExport(); _form.update { it.copy(backupPath=file.absolutePath) } }
    fun backupShared() { _form.update { it.copy(backupPath=null) } }
    fun detach()=action { coordinator.detachAccount(true) }
    fun listDevices()=action { val devices=coordinator.devices(); _form.update { it.copy(devices=devices) } }
    fun releaseWriter(id: String)=action { coordinator.releaseWriter(id); closeReview() }
    fun logout()=action { coordinator.logout() }
    fun deferOnboarding(value: Boolean)=coordinator.deferForOnboarding(value)
}

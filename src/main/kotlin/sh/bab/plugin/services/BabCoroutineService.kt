package sh.bab.plugin.services

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class BabCoroutineService(val scope: CoroutineScope)

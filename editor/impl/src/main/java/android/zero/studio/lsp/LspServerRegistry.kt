package android.zero.studio.lsp

import android.zero.studio.lsp.BaseLspServer
import java.util.concurrent.ConcurrentHashMap

//lsp server
import android.zero.studio.lsp.servers.workspace.WorkspaceSymbolServer

/**
 * Registry for managing available Language Servers.
 * Thread-safe singleton.
 *
 * @author android_zero
 */
object LspServerRegistry {
    private val servers = ConcurrentHashMap<String, BaseLspServer>()
    private val extensionMap = ConcurrentHashMap<String, String>() // extension -> serverId

    init {
        // register LSP servers
        register(WorkspaceSymbolServer())
        
    }

    fun register(server: BaseLspServer) {
        servers[server.id] = server
        server.supportedExtensions.forEach { ext ->
            // Last registered server for an extension wins (or handle priority logic here)
            extensionMap[ext.lowercase()] = server.id
            
            val key = ext.lowercase()
            // Important: Don't let the wildcard override specific servers.
            // Only add if the extension is not already mapped.
            if (key != "*" && !extensionMap.containsKey(key)) {
                extensionMap[key] = server.id
            }
            
        }
    }

    fun unregister(serverId: String) {
        val server = servers.remove(serverId)
        if (server != null) {
            // Remove extensions mapping to this server
            val iterator = extensionMap.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().value == serverId) {
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Finds a matching server for a given file extension.
     */
    fun findServerForExtension(ext: String): BaseLspServer? {
        // val serverId = extensionMap[ext.lowercase()] ?: return null
        // return servers[serverId]
        
        val serverId = extensionMap[ext.lowercase()]
        if (serverId != null) {
            return servers[serverId]
        }
        
        // If no specific server, find the generic workspace server
        return servers.values.find { it is WorkspaceSymbolServer }
        
    }

    fun getServer(id: String): BaseLspServer? = servers[id]
    
    fun getAllServers(): Collection<BaseLspServer> = servers.values
}
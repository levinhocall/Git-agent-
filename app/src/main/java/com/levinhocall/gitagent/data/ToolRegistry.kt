package com.levinhocall.gitagent.data

enum class ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class ToolApprovalState {
    AUTO_APPROVED,
    REQUIRES_USER_APPROVAL,
    BLOCKED
}

data class ToolMetadata(
    val name: String,
    val description: String,
    val parameters: List<String>,
    val requiredPermissions: List<String>,
    val riskLevel: ToolRiskLevel,
    val approvalState: ToolApprovalState
)

sealed interface ToolExecutionResult {
    data class Success(val output: String) : ToolExecutionResult
    data class Unsupported(val reason: String) : ToolExecutionResult
}

fun interface ToolExecutor {
    suspend fun execute(args: Map<String, String>): ToolExecutionResult
}

data class RegisteredTool(
    val metadata: ToolMetadata,
    val executor: ToolExecutor
)

class ToolRegistry(private val tools: Map<String, RegisteredTool>) {
    fun listTools(): List<ToolMetadata> = tools.values.map { it.metadata }

    suspend fun execute(name: String, args: Map<String, String>): ToolExecutionResult {
        val tool = tools[name] ?: return ToolExecutionResult.Unsupported("Tool '$name' is not registered")
        return if (tool.metadata.approvalState == ToolApprovalState.BLOCKED) {
            ToolExecutionResult.Unsupported("Tool '$name' is blocked")
        } else {
            tool.executor.execute(args)
        }
    }

    companion object {
        fun phaseOneDefault(): ToolRegistry {
            val appStatusTool = RegisteredTool(
                metadata = ToolMetadata(
                    name = "app.status",
                    description = "Returns local app readiness status",
                    parameters = emptyList(),
                    requiredPermissions = emptyList(),
                    riskLevel = ToolRiskLevel.LOW,
                    approvalState = ToolApprovalState.AUTO_APPROVED
                ),
                executor = ToolExecutor {
                    ToolExecutionResult.Success("AURIX Phase 1 local-only status: tools/web/device actions are not yet implemented")
                }
            )
            val webSearchPlaceholder = RegisteredTool(
                metadata = ToolMetadata(
                    name = "web.search",
                    description = "Web search tool placeholder",
                    parameters = listOf("query"),
                    requiredPermissions = listOf("android.permission.INTERNET"),
                    riskLevel = ToolRiskLevel.MEDIUM,
                    approvalState = ToolApprovalState.REQUIRES_USER_APPROVAL
                ),
                executor = ToolExecutor {
                    ToolExecutionResult.Unsupported("web.search is not implemented in Phase 1")
                }
            )
            return ToolRegistry(mapOf(appStatusTool.metadata.name to appStatusTool, webSearchPlaceholder.metadata.name to webSearchPlaceholder))
        }
    }
}

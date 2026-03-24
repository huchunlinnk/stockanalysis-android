package com.example.stockanalysis.data.agent

/**
 * Agent工具接口
 * 所有Agent可调用的工具实现此接口
 */
interface AgentTool {
    /**
     * 工具名称
     */
    val name: String
    
    /**
     * 工具描述
     */
    val description: String
    
    /**
     * 工具参数定义
     */
    val parameters: ToolParameters
    
    /**
     * 执行工具
     * 
     * @param params 参数映射
     * @return 执行结果
     */
    suspend fun execute(params: Map<String, Any>): Result<String>
}

/**
 * 工具参数定义
 */
data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ParameterProperty>,
    val required: List<String> = emptyList()
)

/**
 * 参数属性
 */
data class ParameterProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

/**
 * 工具调用结果
 */
data class ToolCallResult(
    val toolName: String,
    val params: Map<String, Any>,
    val result: String,
    val isSuccess: Boolean,
    val error: String? = null
)

/**
 * Agent工具注册表
 */
class AgentToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()
    
    fun register(tool: AgentTool) {
        tools[tool.name] = tool
    }
    
    fun getTool(name: String): AgentTool? {
        return tools[name]
    }
    
    fun getAllTools(): List<AgentTool> {
        return tools.values.toList()
    }
    
    fun getToolsForLLM(): List<Map<String, Any>> {
        return tools.values.map { tool ->
            mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to mapOf(
                        "type" to tool.parameters.type,
                        "properties" to tool.parameters.properties.mapValues { (_, prop) ->
                            val map = mutableMapOf<String, Any>(
                                "type" to prop.type,
                                "description" to prop.description
                            )
                            prop.enum?.let { map["enum"] = it }
                            map
                        },
                        "required" to tool.parameters.required
                    )
                )
            )
        }
    }
}

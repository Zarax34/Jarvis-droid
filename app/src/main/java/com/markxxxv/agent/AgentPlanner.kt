package com.markxxxv.agent

import com.markxxxv.api.GeminiApiClient

class AgentPlanner(private val apiClient: GeminiApiClient) {
    
    data class PlanStep(
        val stepNumber: Int,
        val action: String,
        val target: String,
        val params: Map<String, Any>
    )
    
    suspend fun createPlan(goal: String, context: Map<String, Any> = emptyMap()): Result<List<PlanStep>> {
        val systemPrompt = buildString {
            appendLine("You are a planning agent. Break down the user's goal into numbered steps.")
            appendLine("For each step, specify: action, target, and parameters.")
            appendLine("Actions: open_app, open_url, execute_command, search, take_screenshot, etc.")
            appendLine("Return ONLY a JSON array like:")
            appendLine("""[{"step": 1, "action": "open_url", "target": "youtube.com", "params": {}}, ...]""")
        }
        
        val userPrompt = buildString {
            appendLine("Goal: $goal")
            appendLine("Context: $context")
        }
        
        val response = apiClient.generateContent(
            contents = listOf(
                com.markxxxv.api.Content("user", listOf(com.markxxxv.api.Part(text = userPrompt)))
            )
        )
        
        return response.mapCatching { resp ->
            parseSteps(resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "")
        }
    }
    
    private fun parseSteps(jsonText: String): List<PlanStep> {
        return try {
            val regex = Regex("""\{"step":\s*(\d+),\s*"action":\s*"(\w+)",\s*"target":\s*"([^"]*)",\s*"params":\s*(\{[^}]*\})}""")
            regex.findAll(jsonText).map { match ->
                PlanStep(
                    stepNumber = match.groupValues[1].toInt(),
                    action = match.groupValues[2],
                    target = match.groupValues[3],
                    params = emptyMap()  // Simplified
                )
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class AgentExecutor(
    private val planner: AgentPlanner,
    private val tools: ToolExecutor
) {
    
    private val maxRetries = 3
    
    suspend fun executePlan(plan: List<AgentPlanner.PlanStep>): Result<List<String>> {
        val results = mutableListOf<String>()
        
        for (step in plan) {
            var attempt = 0
            var success = false
            var lastError: String? = null
            
            while (attempt < maxRetries && !success) {
                attempt++
                
                val result = tools.execute(step.action, step.target, step.params)
                if (result.isSuccess) {
                    success = true
                    results.add("Step ${step.stepNumber}: ${result.getOrNull()}")
                    
                    // Inject context for next step
                    if (step.stepNumber < plan.size) {
                        val nextStep = plan[step.stepNumber]
                        // Could modify next step params with context
                    }
                } else {
                    lastError = result.exceptionOrNull()?.message
                    // Analyze error and decide retry/skip/replan
                }
            }
            
            if (!success) {
                results.add("Step ${step.stepNumber} failed: $lastError")
            }
        }
        
        return if (results.any { it.startsWith("failed") }) {
            Result.failure(Exception("Plan failed"))
        } else {
            Result.success(results)
        }
    }
}

class ToolExecutor(private val context: android.content.Context) {
    
    fun execute(action: String, target: String, params: Map<String, Any>): Result<String> {
        return try {
            val result = when (action) {
                "open_url" -> com.markxxxv.tools.BrowserTools(context).openUrl(target)
                "open_app" -> com.markxxxv.tools.AppTools(context).openApp(target)
                "search" -> com.markxxxv.tools.BrowserTools(context).searchGoogle(target)
                "volume" -> com.markxxxv.tools.SystemTools(context).setVolume(target.toIntOrNull() ?: 50)
                "screenshot" -> com.markxxxv.tools.SystemTools(context).takeScreenshot()
                else -> "Unknown action: $action"
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
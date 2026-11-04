package com.FreeRave.remindme.screens.ai

import com.FreeRave.remindme.service.AiServiceUnavailableException
import com.FreeRave.remindme.service.GeminiService
import com.FreeRave.remindme.service.AiRateLimitException
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal class GeminiChatGateway(private val service: GeminiService) : AiChatGateway {
    override suspend fun reply(history: List<ChatMessage>): AiChatReply = try {
        val response = service.generateChatReply(conversationForAi(history)).trim()
        val command = reminderCommand(response)
        if (command != null) {
            val draft = parseAiReminder(service.parseReminderToJSON(command), command, System.currentTimeMillis())
                ?: throw AiChatException(AiChatError.RESPONSE)
            AiChatReply(draft = draft)
        } else {
            if (response.isBlank() || response.contains("ACTION_PARSE_REMINDER:")) throw AiChatException(AiChatError.RESPONSE)
            AiChatReply(text = response)
        }
    } catch (e: CancellationException) { throw e }
    catch (e: AiChatException) { throw e }
    catch (_: AiRateLimitException) { throw AiChatException(AiChatError.RATE_LIMIT) }
    catch (_: AiServiceUnavailableException) { throw AiChatException(AiChatError.UNAVAILABLE) }
    catch (_: IOException) { throw AiChatException(AiChatError.NETWORK) }
}

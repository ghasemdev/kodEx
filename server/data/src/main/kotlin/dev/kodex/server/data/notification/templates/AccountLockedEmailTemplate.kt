package dev.kodex.server.data.notification.templates

@Suppress("StringTemplateIndent")
object AccountLockedEmailTemplate {
    fun render(lockedUntilDisplay: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><title>Your KodEx account is temporarily locked</title></head>
        <body style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:32px;color:#111827">
          <h2 style="margin-bottom:8px">Account temporarily locked</h2>
          <p style="color:#374151">We detected too many failed sign-in attempts on your account. For your
            security, it has been temporarily locked until <strong>$lockedUntilDisplay</strong>.</p>
          <p style="color:#374151">If this wasn't you, consider changing your password once the lock expires.</p>
          <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0">
          <p style="color:#9ca3af;font-size:12px">If you recognize this activity, no action is needed — you can
            sign in again after the lock expires.</p>
        </body>
        </html>
    """.trimIndent()
}

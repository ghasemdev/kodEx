package dev.kodex.server.data.notification.templates

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#x27;")

@Suppress("StringTemplateIndent")
object VerificationEmailTemplates {
    fun magicLink(link: String): String {
        val safeHref = link.escapeHtml()
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><title>Verify your KodEx email</title></head>
            <body style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:32px;color:#111827">
              <h2 style="margin-bottom:8px">Welcome to KodEx!</h2>
              <p style="color:#374151">Click the button below to verify your email address. This link expires in <strong>1 hour</strong>.</p>
              <a href="$safeHref"
                 style="display:inline-block;margin:16px 0;padding:12px 28px;background:#6366f1;color:#fff;border-radius:8px;text-decoration:none;font-weight:600;font-size:15px">
                Verify my email
              </a>
              <p style="color:#6b7280;font-size:13px">Or copy this link into your browser:<br>
                <a href="$safeHref" style="color:#6366f1;word-break:break-all">$safeHref</a>
              </p>
              <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0">
              <p style="color:#9ca3af;font-size:12px">If you didn't create a KodEx account, you can safely ignore this email.</p>
            </body>
            </html>
        """.trimIndent()
    }

    fun otp(code: String): String {
        val safeCode = code.escapeHtml()
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><title>Your KodEx verification code</title></head>
            <body style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:32px;color:#111827">
              <h2 style="margin-bottom:8px">Your verification code</h2>
              <p style="color:#374151">Enter this code in the app to verify your email. It expires in <strong>2 minutes</strong>.</p>
              <div style="display:inline-block;margin:20px 0;padding:18px 36px;background:#f3f4f6;border-radius:12px;font-size:36px;font-weight:700;letter-spacing:10px;color:#111827">
                $safeCode
              </div>
              <p style="color:#6b7280;font-size:13px">Do not share this code with anyone.</p>
              <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0">
              <p style="color:#9ca3af;font-size:12px">If you didn't create a KodEx account, you can safely ignore this email.</p>
            </body>
            </html>
        """.trimIndent()
    }
}

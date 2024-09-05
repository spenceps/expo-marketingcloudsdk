package expo.modules.marketingcloudsdk

import android.app.Application
import android.content.Context
import android.util.Log
import android.app.PendingIntent
import com.salesforce.marketingcloud.MCLogListener
import com.salesforce.marketingcloud.MarketingCloudConfig
import com.salesforce.marketingcloud.MarketingCloudSdk
import com.salesforce.marketingcloud.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.sfmcsdk.BuildConfig
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener
import com.salesforce.marketingcloud.notifications.NotificationManager
import expo.modules.core.interfaces.ApplicationLifecycleListener
import kotlin.random.Random

class ExpoMarketingCloudSdkApplicationLifecycleListener : ApplicationLifecycleListener {
  override fun onCreate(application: Application) {
    // Initialize logging _before_ initializing the SDK to avoid losing valuable debugging information.
    if(getDebug(application)) {
      SFMCSdk.setLogging(LogLevel.DEBUG, LogListener.AndroidLogger())
      MarketingCloudSdk.setLogLevel(MCLogListener.VERBOSE)
      MarketingCloudSdk.setLogListener(MCLogListener.AndroidLogListener())
      SFMCSdk.requestSdk { sdk ->
        sdk.mp { push ->
          push.registrationManager.registerForRegistrationEvents {
            // Log the registration on successful sends to the MC
            Log.i("~#SFMC-expo", "Registration: $it")
          }
        }
      }
    }

    // Configure Salesforce Marketing Cloud SDK
    SFMCSdk.configure(application, SFMCSdkModuleConfig.build {
      pushModuleConfig = MarketingCloudConfig.builder().apply {
        setApplicationId(getAppId(application))
        setAccessToken(getAccessToken(application))
        setAnalyticsEnabled(getAnalyticsEnabled(application))
        setMarketingCloudServerUrl(getServerUrl(application))
        setDelayRegistrationUntilContactKeyIsSet(getDelayRegistrationUntilContactKeyIsSet(application))
        if(getSenderId(application) != "") setSenderId(getSenderId(application))
        setInboxEnabled(getInboxEnabled(application))
        setMarkMessageReadOnInboxNotificationOpen(getMarkMessageReadOnInboxNotificationOpen(application))
        setNotificationCustomizationOptions(getNotificationOptions(application))
      }.build(application)
    }) { initStatus ->
      // TODO handle initialization status
      Log.e("SMFCSdk: initStatus", initStatus.toString())
    }
  }

  private fun getNotificationOptions(context: Context): NotificationCustomizationOptions {
    return NotificationCustomizationOptions.create { context, notificationMessage ->
      val builder = NotificationManager.getDefaultNotificationBuilder(
        context,
        notificationMessage,
        NotificationManager.createDefaultNotificationChannel(context),
        R.drawable.notification_icon
      )
      builder.setContentIntent(
        NotificationManager.redirectIntentForAnalytics(
          context,
          PendingIntent.getActivity(
            context,
            Random.Default.nextInt(),
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
              // Add custom keys to the intent
              notificationMessage.customKeys.forEach { (key, value) ->
                putExtra(key, value)
              }
              if (!notificationMessage.url.isNullOrEmpty()) {
                putExtra("url", notificationMessage.url)
              }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          ),
          notificationMessage,
          true
        )
      )
    }
  }
  private fun getDebug(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_debug) == "true"
  private fun getAppId(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_app_id)
  private fun getAccessToken(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_access_token)
  private fun getServerUrl(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_server_url)
  private fun getSenderId(context: Context): String = context.resources.getString(R.string.expo_marketingcloudsdk_sender_id)
  private fun getAnalyticsEnabled(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_analytics_enabled) == "true"
  private fun getDelayRegistrationUntilContactKeyIsSet(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_delay_registration_until_contact_key_is_set) == "true"
  private fun getInboxEnabled(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_inbox_enabled) == "true"
  private fun getMarkMessageReadOnInboxNotificationOpen(context: Context): Boolean = context.resources.getString(R.string.expo_marketingcloudsdk_mark_message_read_on_inbox_notification_open) == "true"
}
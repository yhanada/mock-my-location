package com.github.yhanada.mockmylocation

import android.Manifest
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yhanada.mockmylocation.model.MyLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MockMyLocationService : Service() {

    companion object {
        private const val CHANNEL_ID = "MOCK_MY_LOCATION"
        private const val CHANNEL_NAME = "My位置偽装"

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_TITLE = "MockMyLocation"

        private const val INTENT_ACTION_START = "INTENT_ACTION_START"
        private const val INTENT_ACTION_STOP = "INTENT_ACTION_STOP"
        private const val INTENT_EXTRA_LOCATION = "INTENT_EXTRA_LATITUDE"
        private const val INTENT_EXTRA_PACKAGE_NAME = "INTENT_EXTRA_PACKAGE_NAME"
        private const val INTERVAL = 1_000L

        fun start(context: Context, location: MyLocation) {
            val intent = Intent(context, MockMyLocationService::class.java).apply {
                action = INTENT_ACTION_START
                putExtra(INTENT_EXTRA_LOCATION, location)
                putExtra(INTENT_EXTRA_PACKAGE_NAME, context.packageName)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MockMyLocationService::class.java).apply {
                action = INTENT_ACTION_STOP
            }
            context.startService(intent)
        }

    }

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(applicationContext)
    }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisorJob)
    private var job: Job? = null

    private var latitude: Double? = null
    private var longitude: Double? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            INTENT_ACTION_START -> {
                val location = intent.getParcelableExtra<MyLocation>(INTENT_EXTRA_LOCATION)
                val packageName = intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME).orEmpty()
                startMockLocation(location, packageName)
            }
            INTENT_ACTION_STOP -> {
                stopMockLocation()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startMockLocation(location: MyLocation?, packageName: String) {
        location ?: return

        if (!isMockAppEnabled(packageName)) {
            // 仮の位置情報アプリに設定されていない
            showToast("位置情報を設定するには仮の位置情報アプリに設定してください")
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        }

        clearMockLocation()

        if (setMockLocation(location.lat, location.lng)) {
            this.latitude = location.lat
            this.longitude = location.lng
            showToast("位置情報の偽装を開始しました")
            updateNotification("mock my location is $location")
            job?.cancel()
            job = scope.launch {
                while (true) {
                    delay(INTERVAL)
                    if (!setMockLocation(location.lat, location.lng)) stopMockLocation()
                }
            }
        } else {
            showToast("位置情報の偽装に失敗しました")
        }
    }

    private fun stopMockLocation() {
        job?.cancel()
        clearMockLocation()
        removeNotification()
        showToast("位置情報の偽装を終了しました")
        stopSelf()
    }

    private fun isMockAppEnabled(packageName: String): Boolean {
        try {
            val opsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            return opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } catch (e: SecurityException) {
            // MOCK_LOCATIONにアプリが設定されていないとcheckOpでSecurityExceptionが発生する
            return false
        }
    }

    private fun setMockLocation(latitude: Double, longitude: Double): Boolean {
        try {
            for (provider in providers) {
                locationManager.run {
                    addTestProvider(
                        provider,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                    setTestProviderEnabled(provider, true)
                    setTestProviderLocation(provider, Location(provider).apply {
                        this.latitude = latitude
                        this.longitude = longitude
                        this.altitude = 0.0
                        this.accuracy = 500f
                        this.time = System.currentTimeMillis()
                        this.speed = 0f
                        this.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                    })
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun clearMockLocation() {
        try {
            for (provider in providers) {
                locationManager.run {
                    removeTestProvider(provider)
                }
            }
        } catch (e: IllegalArgumentException) {
            // addTestProviderされていない状態でremoveTestProviderを呼ぶとIllegalArgumentExceptionが発生
        }
    }

    private fun showToast(message: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun registerNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val contentText = "Running now."
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID).run {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(NOTIFICATION_TITLE)
            setContentText(contentText)
            build()
        }
    }

    private fun updateNotification(text: String) {
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(applicationContext, MockMyLocationService::class.java).apply {
                action = INTENT_ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val action = NotificationCompat.Action(R.mipmap.ic_launcher, "位置偽装を終了する", stopPendingIntent)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).run {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(NOTIFICATION_TITLE)
            setContentText(text)
            addAction(action)
            build()
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun removeNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

}

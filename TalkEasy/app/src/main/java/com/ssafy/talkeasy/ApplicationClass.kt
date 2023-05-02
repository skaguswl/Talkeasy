package com.ssafy.talkeasy

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility

class ApplicationClass : Application() {

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this@ApplicationClass, getString(R.string.KAKAO_NATIVE_APP_KEY))
        Log.d("KaKao-KeyHash", Utility.getKeyHash(this@ApplicationClass))
    }
}
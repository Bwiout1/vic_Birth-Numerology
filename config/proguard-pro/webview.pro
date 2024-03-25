-dontwarn android.webkit**

# WebView(可选)
-keepclassmembers class * extends android.webkit.WebView {
   public *;
}

# WebView的复杂操作
-keepclassmembers class * extends android.webkit.WebViewClient {
     public void *(android.webkit.WebView,java.lang.String,android.graphics.Bitmap);
     public boolean *(android.webkit.WebView,java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
     public void *(android.webkit.WebView,java.lang.String);
}

# 与JS交互
-keepattributes SetJavaScriptEnabled
-keepattributes JavascriptInterface

# 保留与JS交互接口 , API17+
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
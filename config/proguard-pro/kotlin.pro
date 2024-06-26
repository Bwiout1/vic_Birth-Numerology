# Allow R8 to optimize away the FastServiceLoader.
# Together with ServiceLoader optimization in R8
# this results in direct instantiation when loading Dispatchers.Main
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}

-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoaderKt {
    boolean ANDROID_DETECTED return true;
}

# Disable support for "Missing Main Dispatcher", since we always have Android main dispatcher
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatchersKt {
    boolean SUPPORT_MISSING return false;
}

# Statically turn off all debugging facilities and assertions
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED() return false;
    boolean getDEBUG() return false;
    boolean getRECOVER_STACK_TRACES() return false;
}

# Ensure the DebugMetadata annotation is not included in the APK.
#-checkdiscard @interface kotlin.coroutines.jvm.internal.DebugMetadata
#-assumenosideeffects public class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
#  private kotlin.coroutines.jvm.internal.DebugMetadata getDebugMetadataAnnotation() return null;
#  public java.lang.StackTraceElement getStackTraceElement() return null;
#  public java.lang.String[] getSpilledVariableFieldMapping() return null;
#}
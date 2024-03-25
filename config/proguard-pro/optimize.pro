# 指定混淆时采用的算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# 代码混淆的压缩比例(0-7) , 默认为5 , 一般不需要改
-optimizationpasses 5

# 混淆后类名都小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共的库的类(即混淆第三方, 第三方库可能自己混淆了 , 可在后面配置某些第三方库不混淆)
# 默认跳过，有些情况下编写的代码与类库中的类在同一个包下，并且持有包中内容的引用，此时就需要加入此条声明
-dontskipnonpubliclibraryclasses

# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers

# 不做预检验，preverify是proguard的四个步骤之一
# Android不需要preverify，去掉这一步可以加快混淆速度
-dontpreverify

# 有了verbose这句话，混淆后就会生成映射文件
# 包含有类名->混淆后类名的映射关系
# 然后使用printmapping指定映射文件的名称
-verbose
-printmapping proguardMapping.txt

# 保护代码中的Annotation不被混淆
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
# 这在JSON实体映射时非常重要，比如fastJson
-keepattributes Signature

#抛出异常时保留源文件和代码行号
#-keepattributes SourceFile,LineNumberTable

#Android SDK默认的proguard-android-optimize.txt用*,但用某些版本的AS和Gradle时,不能处理内嵌enum的情况
-keepclassmembers enum ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
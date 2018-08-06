package com.kibey.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import sun.reflect.misc.FieldUtil

class PluginTransform extends Transform {

    Project project

    public PluginTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(
            Context context,
            Collection<TransformInput> inputs,
            Collection<TransformInput> referencedInputs,
            TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {

        // Transform的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
        inputs.each { TransformInput input ->
            //对类型为“文件夹”的input进行遍历
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等

                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                boolean success = process(directoryInput.file, project.rootDir.getPath() + "/path.jar")
                if (success) {
                    Logs.d("dest = " + dest)
                }
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->

                //jar文件一般是第三方依赖库jar文件

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //生成输出路径
                def dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
                if (!inIgnore(jarInput.name)) {
                    Logs.d("=================================jarName=" + jarInput.name)
                    if (jarInput.name.startsWith(":")) {
                        String name = jarInput.name.replace(":", "")
                        File classDir = new File(project.rootDir.getAbsolutePath() + "/.test/temp/" + name)
                        File newJarFile = new File(project.rootDir.getAbsolutePath() + "/.test/temp/" + name + ".jar")
                        classDir.mkdirs()
                        Decompression.uncompress(jarInput.file, classDir)
                        boolean success = process(classDir, project.rootDir.getPath() + "/path.jar")
                        if (success) {
                            Thread.sleep(1000)
                            Compressor compressor = new Compressor(newJarFile.getAbsolutePath())
                            compressor.compress(classDir.getAbsolutePath())
                            Logs.d("classDir = " + classDir.getPath() + " jar name = " + jarInput.name)
                            Logs.d("dest = " + dest)
                            Logs.d("newJar = " + newJarFile + " " + newJarFile.exists())
                            FileUtils.copyFile(newJarFile, jarInput.file)
                        } else {
                            FileUtils.deleteDirectory(classDir)
                        }
                    }
                    //将输入内容复制到输出
                    FileUtils.copyFile(jarInput.file, dest)
                } else {
//                    File classDir = new File(project.rootDir.getAbsolutePath() + "/.test/temp/" + md5Name)
//                    File newJarFile = new File(project.rootDir.getAbsolutePath() + "/.test/temp/" + md5Name + ".jar")
//                    classDir.mkdirs()
//                    Decompression.uncompress(jarInput.file, classDir)
//                    deleteNotRClass(classDir)
//                    Compressor compressor = new Compressor(newJarFile.getAbsolutePath())
//                    compressor.compress(classDir.getAbsolutePath())
//                    FileUtils.copyFile(newJarFile, jarInput.file)
//                    FileUtils.deleteDirectory(classDir)
                    Logs.d("不打包：" + jarInput.name + " " + jarInput.file.getPath() +" "+ dest.exists())
                    dest.delete()
//                    FileUtils.copyFile(newJarFile, dest)
                }

            }
        }
    }

    void deleteNotRClass(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles()
            for (int i = 0; i < files.length; i++) {
                deleteNotRClass(files[i])
            }
        } else {
            if (dir.getName() != "R") {
                dir.delete()
            }
        }
    }

    boolean inIgnore(String name) {
        if (name.startsWith(":")) {
            return mIgnoreList.contains(name)
        } else {
            name = name.substring(0, name.lastIndexOf(":") + 1)
            return mIgnoreList.contains(name)
        }
    }

    @Override
    String getName() {
        return PluginTransform.simpleName
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    static Map<String, Boolean> mHasProcessed = new HashMap<>()

    static List<String> mFixClassList = new ArrayList<>()
    static List<String> mIgnoreList = new ArrayList<>()

    static {
        mFixClassList.add("com.kibey.plugin.ui.PluginPage")
        mFixClassList.add("com.kibey.plugin.ui.holder.PluginHolder")
        mFixClassList.add("com.kibey.android.data.net.HttpSubscriber")
        mFixClassList.add("com.kibey.android.data.model.MitcModel")
        mFixClassList.add("com.kibey.android.data.model.BaseResponse")

        mIgnoreList.add(":common-widget")
        mIgnoreList.add(":common-router")
        mIgnoreList.add(":common-utils")
        mIgnoreList.add(":common-model")
        mIgnoreList.add(":router")
        mIgnoreList.add(":base")
        mIgnoreList.add(":lib_gif")
        mIgnoreList.add(":account")
        mIgnoreList.add(":proxy_release")
        mIgnoreList.add(":sweet-dialog")
        mIgnoreList.add(":materialish-progress")
        mIgnoreList.add(":base_release")
        mIgnoreList.add("io.reactivex:rxandroid:")
        mIgnoreList.add("io.reactivex:rxjava:")
        mIgnoreList.add("org.jetbrains.kotlin:kotlin-stdlib-jdk7:")
        mIgnoreList.add("com.android.support:multidex:")
        mIgnoreList.add("org.jetbrains.kotlin:kotlin-stdlib:")
        mIgnoreList.add("com.squareup.retrofit2:converter-gson:")
        mIgnoreList.add("de.hdodenhof:circleimageview:")
        mIgnoreList.add("com.yqritc:recyclerview-flexibledivider:")
        mIgnoreList.add("org.jetbrains.kotlin:kotlin-stdlib-common:")
        mIgnoreList.add("org.jetbrains:annotations:")
        mIgnoreList.add("com.squareup.retrofit2:retrofit:")
        mIgnoreList.add("com.google.code.gson:gson:")
        mIgnoreList.add("com.chenenyu.router:annotation:")
        mIgnoreList.add("com.squareup.okhttp3:okhttp:")
        mIgnoreList.add("com.squareup.okio:okio:")
        mIgnoreList.add("org.greenrobot:greendao:")
        mIgnoreList.add("com.burgstaller:okhttp-digest:")
        mIgnoreList.add("org.greenrobot:greendao-api:")
        mIgnoreList.add("com.jakewharton:butterknife:")
        mIgnoreList.add("com.jakewharton:butterknife-annotations:")
        mIgnoreList.add("com.android.support:support-annotations:")
        mIgnoreList.add("com.android.support:support-core-utils:")
        mIgnoreList.add("com.android.support:support-media-compat:")
        mIgnoreList.add("com.android.support:support-fragment:")
        mIgnoreList.add("com.android.support:support-v4:")
        mIgnoreList.add("com.android.support:support-core-ui:")
        mIgnoreList.add("com.android.support:animated-vector-drawable:")
        mIgnoreList.add("com.android.support:support-vector-drawable:")
        mIgnoreList.add("com.android.support:transition:")
        mIgnoreList.add("com.android.support:gridlayout-v7:")
        mIgnoreList.add("com.android.support:recyclerview-v7:")
//        mIgnoreList.add("com.android.support:appcompat-v7:")
        mIgnoreList.add("com.squareup.retrofit2:adapter-rxjava:")
        mIgnoreList.add("android.local.jars:zip4j_1.3.2.jar:")
    }
    /**
     * 植入代码
     * @param buildDir 是项目的build class目录,就是我们需要注入的class所在地
     * @param lib 这个是hackdex的目录,就是PluginLoad类的class文件所在地
     */
    public static boolean process(File buildDir, String lib) {
        ClassPool classes = ClassPool.getDefault()
        classes.appendClassPath(buildDir.path)
        classes.appendClassPath(lib)

        //下面的操作比较容易理解,在将需要关联的类的构造方法中插入引用代码

        boolean flag
        mFixClassList.forEach {
            try {
                String clzName = it
                if (true != mHasProcessed.get(clzName)) {
                    CtClass c = classes.getCtClass(clzName)
                    Logs.d("====添加构造方法====" + c.getName() + " " + c.isFrozen())

                    if (c.isFrozen()) {
                        c.defrost()
                    }

                    def constructor = c.getConstructors()[0]
                    constructor.insertAfter("System.out.println(com.tt.PluginLoad.class);")
                    if (buildDir.isDirectory()) {
                        c.writeFile(buildDir.getPath())
                    } else {
                        c.writeFile()
                    }
                    c.freeze()
                    mHasProcessed.put(clzName, true)
                    flag = true
                }
            } catch (Exception e) {

            }
        }
        return flag
    }
}
package com.forgetting.cicd;

/**
 * @author: wang.j86@foxmail.com
 * @version: 1.0
 * @date: 2018-05-10
 */
 
@Grab(group='org.apache.commons', module='commons-lang3', version='3.7')
import org.apache.commons.lang3.StringUtils
import com.forgetting.cicd.CommonUtils
class CIOperation implements Serializable {
    def steps

    CIOperation(steps) {
        this.steps = steps
    }


    def gitCheckout(repoUrl, branch, folderName) {
        try {
            steps.println "----------------------开始从${repoUrl}中拉取文件-------------------------"
            if(StringUtils.isEmpty(folderName)){
                steps.checkout([$class: 'GitSCM', 
                    branches: [[name: "$branch"]], userRemoteConfigs: [[url: "$repoUrl"]]])
            }else{
                steps.dir("$folderName") {
                    steps.checkout([$class: 'GitSCM', 
                        branches: [[name: "$branch"]], userRemoteConfigs: [[url: "$repoUrl"]]])
                }
                
            }
        } catch (e) {
            steps.println "----------------------拉取源码failed!----------------------"
            throw e
        }
    }

    def mvnBuild(isSkipTest, isFailureIgnore, compilePara, folderName) {
        try {
            steps.println '----------------------开始Maven执行代码打包-------------------------'
            def mvnHome = steps.tool 'M3'
            def runScript = "'${mvnHome}/bin/mvn' "
            if(isSkipTest == true){
                runScript = runScript.concat("-Dmaven.test.skip=true ")    
            }
            if(isFailureIgnore == true){
                runScript = runScript.concat("-Dmaven.test.failure.ignore=true ")    
            }
            if(StringUtils.isEmpty(compilePara)){
                runScript = runScript.concat("clean package -U -e -X")    
            }else{
                runScript = runScript.concat("clean package ${compilePara}")    
            }
            if(StringUtils.isEmpty(folderName)){
                steps.sh "$runScript"
            }else{
                steps.dir("$folderName") {
                    steps.sh "$runScript"
                }
            }
        } catch (e) {
            steps.println '----------------------Maven执行代码打包failed!----------------------'
            throw e 
        }
    }

    def sonarQubeAnalysis(pom, folderName) {
        try {
            steps.println '----------------------开始执行Sonarqube代码质量分析-------------------------'
            def sonarStr = steps.libraryResource "sonar-project.properties"
            sonarStr = sonarStr.replaceAll("ProjectName", pom.artifactId).replaceAll("ProjectVersion", pom.version)
            def scannerHome = steps.tool 'sonar-scanner'
            if(StringUtils.isEmpty(folderName)){
                steps.sh "${scannerHome}/bin/sonar-scanner"
            }else{
                steps.dir("$folderName") {
                    steps.writeFile encoding: 'utf-8', file: "sonar-project.properties", text: sonarStr
                    steps.sh "${scannerHome}/bin/sonar-scanner"
                }
            }
        } catch (e) {
            steps.println '----------------------执行Sonarqube代码质量分析failed!----------------------'
            throw e 
        }
    }

    def deployToNexus(nexusUrl, repoName, pom, folderName) {
        try {
            println '----------------------上传至相应Nexus库-----------------------------'
             if(StringUtils.isEmpty(folderName)){
                def mvnHome = steps.tool 'M3'
                def jarName = "${pom.artifactId}-${pom.version}.jar"
                def script = "'${mvnHome}/bin/mvn' deploy:deploy-file -DgroupId=${pom.groupId} -DartifactId=${pom.artifactId} -Dversion=${pom.version} -Dpackaging=jar -Dfile=./target/${jarName} -Durl=${nexusUrl}/${repoName} -DrepositoryId=${repoName}"
                steps.sh script
            }else{
                steps.dir("$folderName") {
                    def mvnHome = steps.tool 'M3'
                    def jarName = "${pom.artifactId}-${pom.version}.jar"
                    def script = "'${mvnHome}/bin/mvn' deploy:deploy-file -DgroupId=${pom.groupId} -DartifactId=${pom.artifactId} -Dversion=${pom.version} -Dpackaging=jar -Dfile=./target/${jarName} -Durl=${nexusUrl}/${repoName} -DrepositoryId=${repoName}"
                    steps.sh script
                }
            }
           
        } catch(e) {
            println '----------------------上传至相应Nexus库failed!----------------------'
            throw e
        }
    }

    def buildJavaImageAndPush(registryUrl, productName, envName, pom, tagName, dockerfileName, appConfig, folderName) {
        println '----------------------开始制作镜像-----------------------------'
        def imageFullUrl
        try{
            steps.dir("$folderName/target/") {
                def jarName = "${pom.artifactId}-${pom.version}.jar"
                
                if(tagName == null || tagName.equals("")){
                    imageFullUrl= "$registryUrl/$productName/${pom.artifactId}:${pom.version}"
                }else{
                    imageFullUrl= "$registryUrl/$productName/${pom.artifactId}:${tagName}"
                }
                steps.println imageFullUrl
                def dockerfileStr = steps.libraryResource "$dockerfileName"
                dockerfileStr = dockerfileStr.replaceAll("envcicd", envName)
                if (appConfig.privileged) {
                    dockerfileStr = dockerfileStr.replaceAll("USER 185", "")
                }
                steps.writeFile encoding: 'utf-8', file: 'Dockerfile', text: dockerfileStr   
                steps.sh "cp ${jarName} temp.jar"
                steps.withDockerRegistry([url: "$registryUrl"]) {
                    steps.sh "docker build -t ${imageFullUrl} ."
                    steps.sh "docker push ${imageFullUrl}"
                    steps.sh "docker rmi ${imageFullUrl}"
                }
            }
        } catch(e) {
            def imageCount = steps.sh returnStdout: true, script: "docker images | grep ${pom.artifactId} | ${tagName} |wc -l"
            if(imageCount>0){
                steps.sh "docker rmi ${imageFullUrl}"
            }
            println '----------------------制作镜像上传failed!----------------------'
            throw e
        }   
    }
}

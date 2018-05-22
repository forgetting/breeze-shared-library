package com.forgetting.cicd;

import groovy.json.JsonSlurper
import com.forgetting.cicd.Constants
/**
 * @author: wang.j86@foxmail.com
 * @version: 1.0
 * @date: 2018-04-16
 */
class CommonUtils implements Serializable{
    def steps

    CommonUtils(steps) {
        this.steps = steps
    }
    
    def getAppEnvConfig(env, appConfig, envType){
        try{
            steps.println "----------------------开始执行函数getAppEnvConfig-------------------------"
            def envAppConfig
            if(appConfig.get(envType) !=null && appConfig.get(envType).get(env) != null){
                envAppConfig = appConfig.get(envType).get(env)
            }else{
                envAppConfig = null
            }
            
            def defaultAppConfig
            if(appConfig.default !=null && appConfig.default.get(envType) != null){
                defaultAppConfig = appConfig.default.get(envType)
            }else{
                defaultAppConfig = null
            }
            
            if(envAppConfig == null){
                if(defaultAppConfig == null){
                    return null
                }else{
                    return defaultAppConfig
                }
            }else{
                if(defaultAppConfig == null){
                    return envAppConfig
                }else{
                    envAppConfig.each{envConfigKey,envConfigValue ->
                        defaultAppConfig.put(envConfigKey,envConfigValue)
                    }
                    return defaultAppConfig
                }
            }
        } catch (e) {
            steps.println "----------------------Run函数getAppEnvConfig发生failed!-------------------------"
            throw e 
        } 
    }
    


    def getAllAppConfig(productConfig, appConfig, env, envType){
        steps.println "----------------------开始执行函数getAllAppConfig-------------------------"
        try{
            def appEnvConfig = getAppEnvConfig(env, appConfig, envType)
            def defaultConfig = productConfig.default.get(envType)
            def sumConfig = [:]
            if(defaultConfig != null){
                defaultConfig.each{
                    appEnvConfigKey,appEnvConfigValue ->
                    sumConfig.put(appEnvConfigKey,appEnvConfigValue)
                }
            }
            if(appEnvConfig != null){
                appEnvConfig.each{
                    appEnvConfigKey,appEnvConfigValue ->
                    sumConfig.put(appEnvConfigKey,appEnvConfigValue)
                }
            }
            sumConfig.put("emailList",[])
            if(productConfig.default.emailList != null){
                for(int i=0; i<productConfig.default.emailList.size;i++){
                    sumConfig.emailList.add(productConfig.default.emailList.get(i))
                }
            }
            if(appConfig.app.emailList != null){
                for(int i=0; i<appConfig.app.emailList.size;i++){
                    sumConfig.emailList.add(appConfig.app.emailList.get(i))
                }
            }
            return sumConfig
        } catch (e) {
            steps.println "----------------------Run函数getAllAppConfig发生failed!-------------------------"
            throw e 
        } 
        
    }
    
    def removeSnapshotFromPom() {
        try {
            steps.println "----------------------开始修改POM-------------------------"
            def pomStr = steps.readFile encoding: 'utf-8', file: "pom.xml"
            pomStr = pomStr.replaceAll("-SNAPSHOT", "")
            steps.writeFile encoding: 'utf-8', file: "pom.xml", text: pomStr
        } catch (e) {
            steps.println "----------------------修改POM发生failed!-------------------------"
            throw e 
        }
    }
    
    def cleanWorkspace() {
        steps.println "Cleaning workspace"
        steps.deleteDir()
    }    
    
    //替换spring-boot-deployment-template.yaml中的APP_NAME, IMAGE_URL, REQUEST_CPU, REQUEST_MEMORY, LIMIT_CPU, LIMIT_MEMORY, REPLICAS
    def generateDeployYamlFromTemplate(templateName, appName, imageUrl, allAppConfig, folderName) {
        try{
            steps.println "----------------------开始执行函数generateDeployYamlFromTemplate-------------------------"
            def appNameToken = 'APP_NAME'
            def imageUrlToken = 'IMAGE_URL'
            def requestCpuToken = 'REQUEST_CPU'
            def requestMemoryToken = 'REQUEST_MEMORY'
            def limitCpuToken = 'LIMIT_CPU'
            def limitMemoryToken = 'LIMIT_MEMORY'
            def replicasToken = 'REPLICAS_NUM'
            //修改
            def deploymentTemplateStr = steps.libraryResource templateName
            deploymentTemplateStr = deploymentTemplateStr.replaceAll(appNameToken, appName).replaceAll(imageUrlToken, imageUrl)
                .replaceAll(requestCpuToken, allAppConfig.resources.requests.cpu.toString()).replaceAll(requestMemoryToken, allAppConfig.resources.requests.memory)
                .replaceAll(limitCpuToken, allAppConfig.resources.limits.cpu.toString()).replaceAll(limitMemoryToken, allAppConfig.resources.limits.memory)
                .replaceAll(replicasToken, allAppConfig.replicas.toString())
            steps.writeFile encoding: 'utf-8', file: "${folderName}/deployment.yaml", text: deploymentTemplateStr
            steps.println deploymentTemplateStr
            def deploymentYaml = steps.readYaml file: "${folderName}/deployment.yaml"
            steps.sh "rm -rf ${folderName}/deployment.yaml"
            deploymentYaml.spec.template.spec.containers[0].env = allAppConfig.envVar
            def originalVolumes = deploymentYaml.spec.template.spec.volumes
            def originalVolumeMounts = deploymentYaml.spec.template.spec.containers[0].volumeMounts
            def reservedVolumes = []
            def reservedVolumeMounts = []
            for(def needVolumeName : allAppConfig.volumes) {
                for (def item : originalVolumes) {
                    if (item.name.equals(needVolumeName)) {
                        reservedVolumes.add(item)
                        break
                    }
                }
                for (def item : originalVolumeMounts) {
                    if (item.name.equals(needVolumeName)) {
                        reservedVolumeMounts.add(item)
                        break
                    }
                }
            }
            deploymentYaml.spec.template.spec.volumes = reservedVolumes
            deploymentYaml.spec.template.spec.containers[0].volumeMounts = reservedVolumeMounts

            def privileged = allAppConfig.privileged
            privileged = privileged != null ? privileged : false
            deploymentYaml.spec.template.spec.containers[0].securityContext = ["privileged" : privileged]
            deploymentYaml.spec.template.spec.containers[0].command = allAppConfig.command
            steps.writeYaml file: "${folderName}/deployment.yaml", data: deploymentYaml
        } catch (e) {
            steps.println "----------------------Run函数generateDeployYamlFromTemplate发生failed!-------------------------"
            throw e 
        }  
    }
    
    def notifyEmail(mailList) {
        def toList =""
        mailList.each{
            item->toList += (item+";")
        }
        steps.emailext (
            to: toList,
            attachLog: true,
            subject: "Jenkins在执行${steps.env.JOB_NAME}的第${steps.env.BUILD_NUMBER}次执行结果",
            body: "\r\n各位CI/CD工程师：\r\n\t请联系相关人员及时处理本次执行结果，本次执行Log为见附件。\r\n\t本次Build的直接链接为：${steps.env.BUILD_URL}console\r\n\n\nMit Freundlichen Grüßen /Best Regards!\r\n王健     Wangjian     (Mr.)\r\n上期汽车有限公司信息系统部 软件开发股 (CID-1 SVW)\r\nTEL：(021) 695-64826\r\nEMAIL：wang.j86@foxmail.com"
        )
    }

    def getAllGitBranch(git_url){
        def branch = steps.sh returnStdout: true, script: "git ls-remote -h  $git_url|grep -oP '(?<=refs/heads/).*'"
        return branch
    }
}
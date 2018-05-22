package com.forgetting.cicd;

/**
 * @author: wang.j86@foxmail.com
 * @version: 1.0
 * @date: 2018-04-27
 */
class CDOperation implements Serializable {
    
    def steps

    CDOperation(steps) {
        this.steps = steps
    }
    
    def getOpenShiftAccessToken(envName, ocServer) {
        try{
            steps.println "----------------------开始执行getOpenShiftAccessToken-------------------------"
            def credentialsId = "openshift-$envName-admin"
            steps.withCredentials([steps.usernamePassword(credentialsId: "$credentialsId", passwordVariable: 'password', usernameVariable: 'username')]) {
                def ocAccessToken = steps.sh returnStdout: true, script: "(oc login $ocServer --username=$steps.username --password=$steps.password --insecure-skip-tls-verify && oc whoami -t) | sed -n '\$p'" 
                ocAccessToken = ocAccessToken.trim()
                return ocAccessToken  
            }
        } catch (e) {
            steps.println "----------------------Run函数getOpenShiftAccessToken发生failed!-------------------------"
            throw e 
        } 
    }
    
    static def getOcCommandPostfix(ocServer, namespace, ocAccessToken){
        return " -n $namespace --server=${ocServer} --insecure-skip-tls-verify=true --token=${ocAccessToken}"
    }
    
    def existOpenShiftDC(serviceName, ocCommandPostfix) {
        try{
            steps.println "----------------------开始执行existOpenShiftDC-------------------------"
            def dcCount = steps.sh returnStdout: true, script:"oc get dc/$serviceName $ocCommandPostfix 2>&1 | awk '{print \$1}' | grep $serviceName | wc -l"
            return dcCount.trim() != "0"
        } catch (e) {
            steps.println "----------------------Run函数existOpenShiftDC发生failed!-------------------------"
            throw e 
        }    
    }
    
    def updateOpenShiftDC(appName, ocCommandPostfix, folderName) {
        try {
            steps.println "----------------------开始更新OpenShift的DC-------------------------"
            def revisionNumBeforeApply = steps.sh returnStdout: true, script: "oc get dc $appName $ocCommandPostfix| awk 'NR!=1{print \$2}'"
            steps.sh "oc apply -f $folderName/deployment.yaml $ocCommandPostfix"
            def revisionNumAfterApply = steps.sh returnStdout: true, script: "oc get dc $appName $ocCommandPostfix| awk 'NR!=1{print \$2}'"
            if (revisionNumBeforeApply == revisionNumAfterApply) {
                steps.sh "oc rollout latest dc/$appName  $ocCommandPostfix"
            }
        } catch (e) {
            steps.println "----------------------更新OpenShift的DC发生failed!-------------------------"
            throw e
        }
    }
    
    def createOpenShiftDC(ocCommandPostfix, folderName) {
        try {
            steps.println "----------------------开始添加OpenShift的DC-------------------------"
            steps.sh "oc create -f $folderName/deployment.yaml $ocCommandPostfix"
        } catch (e) {
            steps.println "----------------------添加OpenShift的DC发生failed!-------------------------"
            throw e
        }
    }
    
    def createOpenShiftService(appName, ocCommandPostfix) {
        try {
            steps.println "----------------------开始添加OpenShift的Service-------------------------"
            steps.sh "oc create service clusterip $appName --tcp=8080:8080,8443:8443,8778:8778 $ocCommandPostfix"
        } catch (e) {
            steps.println "----------------------添加OpenShift的Service发生failed!----------------------"
            throw e
        }
    }

    def existOpenShiftRoute(appName, ocCommandPostfix) {
        try{
            steps.println "----------------------开始执行existOpenShiftRoute-------------------------"
            def routeCount = steps.sh returnStdout: true, script: "oc get route $appName $ocCommandPostfix 2>&1 | awk '{print \$1}' |grep $appName |wc -l"
            return routeCount.trim() != "0"
        } catch (e) {
            steps.println "----------------------Run函数existOpenShiftRoute发生failed!-------------------------"
            throw e 
        }    
    }
    
    def createOpenShiftRoute(appName, envName, ocCommandPostfix) {
        try {
            steps.println "----------------------开始添加OpenShift的Route-------------------------"
            steps.sh "oc expose service $appName --name=$appName --hostname=$appName.${envName}apps.ocp.com $ocCommandPostfix"
        } catch (e) {
            steps.println "----------------------添加OpenShift的Route发生failed!-------------------------"
            throw e 
        }
    }

    def deleteOpenShiftRoute(appName, ocCommandPostfix) {
        try{
            steps.println "----------------------开始删除OpenShift的Route-------------------------"
            steps.sh "oc delete route $appName $ocCommandPostfix"
        }catch(Exception e){
            steps.println "----------------------删除OpenShift的Route发生failed!----------------------"
            throw e
        }
    }
    
    def moveImageToDiffRegistry(sourceRegistryURL, targetRegistryURL, productName, projectName, tagName) {
        
        try {
            steps.println  '----------------------开始复制镜像-----------------------------'
            def sourceImageName
            def targetImageName
            sourceImageName = "$sourceRegistryURL/$productName/${projectName}:${tagName}"
            targetImageName = "$targetRegistryURL/$productName/${projectName}:${tagName}"
            
            steps.sh "docker pull $sourceImageName"
            steps.sh "docker tag $sourceImageName $targetImageName"
            withDockerRegistry([url: '$targetRegistryURL']) {
                steps.sh "docker push $targetImageName"
            }
            steps.sh "docker rmi $sourceImageName"
            steps.sh "docker rmi $targetImageName"
        } catch (e) {
            steps.println '----------------------复制镜像failed!----------------------'
            throw e 
        }
    }
}

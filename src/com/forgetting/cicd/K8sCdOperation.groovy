package com.forgetting.cicd;

/**
 * @author: wang.j86@foxmail.com
 * @version: 1.0
 * @date: 2018-04-16
 */
class K8sCdOperation implements Serializable {
    private static final String ALI_REGISTRY_SECRET_NAME = "ali-registry"
    private static final String ALI_REGISTRY_VPC_URL = "registry-vpc.cn-shanghai.aliyuncs.com"
    private static final String ALI_REGISTRY_CREDENTIALS_ID = "ali-registry-account"

    private def commonUtils
    private def steps
    private def envName
    private def namespace
    private def k8sCommandPostfix

    K8sCdOperation(steps, envName, namespace) {
        this.commonUtils = new CommonUtils(steps)
        this.steps = steps
        this.envName = envName.toLowerCase()
        this.namespace = namespace.toLowerCase
        this.k8sCommandPostfix = "-n $namespace --context=kubernetes-admin/$envName"
    }

    def existDeployment(appName) {
        def existsDeploy = true
        def deployCount = steps.sh returnStdout: true, script: "kubectl get deploy $appName $k8sCommandPostfix 2>&1 | awk '{print \$1}'| grep $appName |wc -l"
        if (deployCount.trim() == "0") {
            existsDeploy = false
        }
        return existsDeploy
    }

    def existIngress(appName) {
        def existsIngress = true
        def ingressCount = steps.sh returnStdout: true, script: "kubectl get ingress $appName $k8sCommandPostfix 2>&1 | awk '{print \$1}'| grep $appName |wc -l"
        if (ingressCount.trim() == "0") {
            existsIngress = false
        }
        return existsIngress
    }

    def generateDeploymentFormTemplate(appName, imageName, imageTag) {
        def imageFullVpcUrl = "$ALI_REGISTRY_VPC_URL/$namespace/$imageName/$imageTag"
        def k8sDeploymentYaml = getBasicDeployment(appName, imageFullVpcUrl)
        setEnvVars(k8sDeploymentYaml)

    }

    private def getBasicDeployment(appName, imageFullVpcUrl) {
        def appNameToken = '\\$\\{APP_NAME\\}'
        def imageUrlToken = '\\$\\{IMAGE_URL\\}'
        def requestCpuToken = '\\$\\{REQUEST_CPU\\}'
        def requestMemoryToken = '\\$\\{REQUEST_MEMORY\\}'
        def limitCpuToken = '\\$\\{LIMIT_CPU\\}'
        def limitMemoryToken = '\\$\\{LIMIT_MEMORY\\}'
        def replicasToken = '\\$\\{REPLICAS\\}'

        def resources = commonUtils.readResourcesFromJobParam()
        def replicas = commonUtils.readReplicasFromJobParam()
        def deploymentTemplateStr = steps.libraryResource "java-deployment-template.yaml"

        def k8sDeploymentStr = deploymentTemplateStr.replaceAll(appNameToken, appName)
                .replaceAll(imageUrlToken, imageUrl)
                .replaceAll(requestCpuToken, resources.requests.cpu)
                .replaceAll(requestMemoryToken, resources.requests.memory)
                .replaceAll(limitCpuToken, resources.limits.cpu)
                .replaceAll(limitMemoryToken, resources.limits.memory)
                .replaceAll(replicasToken, replicas)
        def k8sDeploymentYaml = steps.readYaml text: k8sDeploymentStr
        return k8sDeploymentYaml
    }

    private def setEnvVars(k8sDeploymentYaml) {
        def envVars = commonUtils.readEnvironmentVariableFromJobParam()
        k8sDeploymentYaml.spec.template.spec.containers[0].env = envVars
    }

    private def selectVolumeMounts() {
        def needVolumeNames = (appConfig.volumes != null && appConfig.volumes.size() != 0) ? appConfig.volumes : defaultConfig.volumes
        println "-------000000------needVolumeNames====$needVolumeNames"
        def reservedVolumes = []
        def reservedVolumeMounts = []
        def deploymentYaml = readYaml file: "${jenkinsFileDir}/spring-boot-deployment.yaml"
        def originalVolumes = deploymentYaml.spec.template.spec.volumes
        def originalVolumeMounts = deploymentYaml.spec.template.spec.containers[0].volumeMounts

        for(def needVolumeName : needVolumeNames) {
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
        println "-------1111------reservedVolumes====$reservedVolumes"
        println "-------22222------reservedVolumeMounts====$reservedVolumeMounts"

        deploymentYaml.spec.template.spec.volumes = reservedVolumes
        deploymentYaml.spec.template.spec.containers[0].volumeMounts = reservedVolumeMounts
        sh "rm -rf ${jenkinsFileDir}/spring-boot-deployment.yaml"
        writeYaml file: "${jenkinsFileDir}/spring-boot-deployment.yaml", data: deploymentYaml
    }
 

    private def checkImagePullSecret() {
        def aliRegistrySecretCount = steps.sh returnStdout: true, script: "kubectl get secret $ALI_REGISTRY_SECRET_NAME $k8sCommandPostfix 2>&1 |awk '{print \$1}' |grep $ALI_REGISTRY_SECRET_NAME |wc -l"
        if (aliRegistrySecretCount.trim() == "0") {
            withCredentials([usernamePassword(credentialsId: "$ALI_REGISTRY_CREDENTIALS_ID", passwordVariable: 'password', usernameVariable: 'username')]) {
                sh "kubectl create secret docker-registry $ALI_REGISTRY_SECRET_NAME --docker-server=$ALI_REGISTRY_VPC_URL --docker-username=$username --docker-password=$password --docker-email=wang.j86@foxmail.com $k8sCommandPostfix"
            }
        }
    }

}
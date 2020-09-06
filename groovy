job("task6-job1") {
  description("This job will pull the github repo on every push, update the container using given Dockerfile and push image to DockerHub")
  
  scm {
   
     github('saipavanbm/devops','master')
     
    
  }
  
  triggers {
    githubPush()
  }
  
  wrappers {
    preBuildCleanup()
  }
   command = """
sudo rm -rf /task4
sudo mkdir /task4
sudo cp -rf * /task4
sudo docker build -t httpdweb:v1 /task4
sudo docker tag httpdweb:v1 03012001/httpdweb:v1
sudo docker push 03012001/httpdweb:v1

"""
  
 steps {
    shell(command)
  }
  
}

job("task6-job2") {
  description("This will run on slave nodes and control K8S.")
  restrictToLabel(String ssh-kube)
  
  triggers {
    authenticationToken('1234')
    upstream('task6-job1', 'SUCCESS')
  }
  
  command = """
if kubectl get pods | grep myweb-deploy
then
echo "Deployment exists"
kubectl rollout restart deploy myweb-deploy
kubectl rollout status deploy myweb-deploy
else
kubectl create -f /root/kube/deploy.yml
kubectl scale deployment myweb-deploy --replicas=3
kubectl expose deployment myweb-deploy --port 80 --type=NodePort
fi
"""
  
  steps {
    shell(command)
  }
  
}

job("task6-job3") {
  description ("It will test if pod is running else send a mail")
  
  triggers {
    upstream('task6-job2', 'SUCCESS')
  }
  steps {
    shell('''if sudo kubectl get deployments myweb-deploy
then
echo "send to production"
else
echo "sending back to developer"
exit 1
fi''')
  }
 
}







buildPipelineView('TASK-6') {
  filterBuildQueue(true)
  filterExecutors(false)
  title('TASK-6')
  displayedBuilds(1)
  selectedJob('task6-job1')
  alwaysAllowManualTrigger(false)
  showPipelineParameters(true)
  refreshFrequency(1)
}

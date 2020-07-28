job("task6_job1"){
description("The First Job: Downloading content from GitHub")
        
scm{
github('devilsm13/DevOps_Task6', 'master')
}
triggers {
scm('* * * * *')
}
steps {
shell('''rm -rvf /root/task3/*
cp -rvf * /root/task3/
''')
}
}

job('task6_job2'){
description("The Second Job: Deploying respective webpages on the server")

triggers {  
upstream('task6_job1', 'SUCCESS')
}
steps{
remoteShell('root@192.168.43.176:22') {
command('''if sudo ls /root/dev3 | grep .html
then
if sudo kubectl get deployment | grep webserver
then
echo "The Web Deployment is already running"
else
sudo kubectl create -f /root/kube/webserver.yml
sleep 6
if sudo kubectl get pods | grep web
then
a=$(sudo kubectl get pods -o 'jsonpath={.items[0].metadata.name}')
sudo kubectl cp /root/dev3/index.html $a:/var/www/html
else
echo "Cannot copy the HTML code"
fi
fi
else
echo "The code is not for HTML"
fi

if sudo ls /root/dev3 | grep .php
then
if sudo kubectl get deployment | grep phpserver
then
echo "The PHP Deployment is already running"
else
sudo kubectl create -f /root/kube/phpserver.yml
sleep 6
if kubectl get pods | grep php
then
b=$(sudo kubectl get pods -o 'jsonpath={.items[0].metadata.name}')
sudo kubectl cp /root/dev3/index.php $b:/var/www/html
else
echo "Cannot copy the PHP code"
fi
fi
else
echo "The code is not for PHP"
fi''')
}
}
}

job("task6_job3"){
description("The Third Job: Testing the environments")

triggers {
upstream('task6_job2','SUCCESS')
}
steps{
remoteShell('root@192.168.43.176:22') {
command('''if sudo kubectl get pods | grep webserver
then
web_status_code=$(curl -o /dev/null -s -w "%{http_code}" 192.168.99.100:31000)
if [[ $web_status_code == 200 ]]
then
echo "The webserver is running fine"
else
echo "Something is wrong with the Web Server"
exit 1
fi
else
echo "No webserver running"
fi

if sudo kubectl get pods | grep phpserver
then
php_status_code=$(curl -o /dev/null -s -w "%{http_code}" 192.168.99.100:32000)
if [[ $php_status_code == 200 ]]
then
echo "The PHP server is working fine"
else
echo "Something is wrong with the PHP server"
exit 1
fi
else
echo "No PHP server running"
fi''')
}
}

publishers {
extendedEmail {
recipientList('shhhhubhammmm@gmail.com')
defaultSubject('Something is wrong with the build')
defaultContent('The testing has been failed. Please Check!!')
contentType('text/html')
triggers {
beforeBuild()
stillUnstable {
subject('Subject')
content('Body')
sendTo {
developers()
requester()
culprits()
}
}
}
}
}
}
#!/bin/bash
set -e

vagrant_dir=/vagrant/vagrant
bashrc=/home/vagrant/.bashrc

echo "========================================"
echo "INSTALLING PERU AND ANSIBLE DEPENDENCIES"
echo "----------------------------------------"
apt-get update
apt-get install -y language-pack-en git unzip libyaml-dev python3-pip python-pip python-yaml python-paramiko python-jinja2

echo "==============="
echo "INSTALLING PERU"
echo "---------------"
sudo pip3 install peru

echo "=================="
echo "INSTALLING ANSIBLE"
echo "------------------"
sudo pip install ansible=='2.2.1.0'

echo "==================================="
echo "CLONING ANSIBLE PLAYBOOKS WITH PERU"
echo "-----------------------------------"
cd ${vagrant_dir} && peru sync -v
echo "... done"

hosts=${vagrant_dir}/ansible.hosts
ssh_checking=False

echo "==================="
echo "CONFIGURING ANSIBLE"
echo "-------------------"
touch ${bashrc}
echo "export ANSIBLE_HOSTS=${hosts}" >> ${bashrc}
echo "export ANSIBLE_HOST_KEY_CHECKING=${ssh_checking}" >> ${bashrc}
echo "... done"

echo "=========================================="
echo "RUNNING PLAYBOOKS WITH ANSIBLE*"
echo "* no output while each playbook is running"
echo "------------------------------------------"
while read pb; do
    su - -c "ansible-playbook ${vagrant_dir}/${pb} --connection=local --inventory-file=${hosts}" vagrant
done <${vagrant_dir}/up.playbooks

guidance=${vagrant_dir}/up.guidance

echo "==========================="
echo "CONFIGURING DOCKER & REDASH"
echo "---------------------------"
sudo curl -L https://github.com/docker/compose/releases/download/1.20.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo docker-compose --version

if [ -f ${guidance} ]; then
    echo "==========="
    echo "PLEASE READ"
    echo "-----------"
    cat $guidance
fi

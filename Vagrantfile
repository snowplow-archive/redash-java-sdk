Vagrant.configure("2") do |config|

  config.vm.box = "ubuntu/trusty64"
  config.vm.hostname = "redash-java-sdk"
  config.ssh.forward_agent = true

  # Required for NFS to work, pick any local IP
  # Use NFS for shared folders for better performance
  config.vm.network :private_network, ip: '192.168.50.50' # Uncomment to use NFS
  config.vm.synced_folder '.', '/vagrant', nfs: true # Uncomment to use NFS
  config.vm.network "forwarded_port", guest: 80, host: 2000

  config.vm.provider :virtualbox do |vb|
    vb.name = Dir.pwd().split("/")[-1] + "-" + Time.now.to_f.to_i.to_s
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    vb.customize [ "guestproperty", "set", :id, "--timesync-threshold", 10000 ]
    # Java is memory-hungry
    vb.memory = 5120
    vb.cpus = 4 # Uncomment to use more cores
  end

  config.vm.provision :shell do |sh|
    sh.path = "vagrant/up.bash"
  end

end

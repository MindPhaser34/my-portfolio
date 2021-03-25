#Creating instance in Hezner Cloud and addind new A-record in AWS DNS to created instance

provider "hcloud" {
  token = var.hcloud_token
}

provider "aws"{
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
  region     = "us-west-2"
}

data "hcloud_image" "snapshot01" {
  with_selector = "type=snapshot01"
}

data "hcloud_ssh_key" "ssh_key_user01" {
  name = "user01@dsk01"
}

data "hcloud_ssh_key" "ssh_key_user02" {
  name = "user02@dsk02"
}

resource "hcloud_server" "project01" {
  count       = length(var.vm_names)
  name        = "${element(var.vm_names, count.index)}.dns.domain.local"
  image       = data.hcloud_image.snapshot01.id
  server_type = var.server_type
  ssh_keys    = ["${data.hcloud_ssh_key.ssh_key_user01.id}","${data.hcloud_ssh_key.ssh_key_user02.id}"]
  
  provisioner "file" {
    connection {
      user = "root"
      host = "${element(hcloud_server.project01.*.ipv4_address, 0)}"
      private_key = "${file("~/.ssh/privkey")}"
    }

    source      = "install-app.sh"
    destination = "/tmp/install-app.sh"
    
  }

  provisioner "remote-exec" {
    connection {
      user = "root"
      host = "${element(hcloud_server.project01.*.ipv4_address, 0)}"
      private_key = "${file("~/.ssh/privkey")}"
    }

    inline = [
      "chmod +x /tmp/install-app.sh",
      "/tmp/install-app.sh",
    ]
  }
}

resource "aws_route53_record" "project01" {
  count       = length(var.vm_names)
  zone_id     = var.zone_id
  name        = "${element(var.vm_names, count.index)}.dns.domain.local"
  type        = "A"
  ttl         = "300"
  records     = ["${element(hcloud_server.project01.*.ipv4_address, count.index)}"]
}

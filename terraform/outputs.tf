
output "created_dynamic_instances" {
    value = formatlist("%s -> %s", "${aws_route53_record.project01.*.name}","${hcloud_server.project01.*.ipv4_address}")
}
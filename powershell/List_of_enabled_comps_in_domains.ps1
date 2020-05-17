$Date = (Get-Date).AddDays(-30)
$Servers = "brn","chlb","rst","ufa","smr","knd","ekb","krsk","srt"

Foreach ($Server in $Servers)
{
$allcomps = (Get-ADComputer -server $Server -Filter * -Properties *) 
	ForEach ($comp in $allcomps)
	{
	If ( $comp.enabled -eq $true -AND $comp.LastLogonDate -ge $Date )
	{$comp | select Name, DNSHostName, OperatingSystem, LastLogonDate | export-csv -Append -path D:\activ_comps.csv -Delimiter ";" -encoding UTF8 -notypeinformation}
	}
Write-host $Server "is done"
}
Write-host "Job is finished"
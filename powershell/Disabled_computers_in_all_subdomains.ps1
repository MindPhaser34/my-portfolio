Function CheckOU { 
	$Status = $false 
	try { if (!([adsi]::Exists("LDAP://$Path"))) 
		{ $Status = $false } 
		else 
		{ $Status = $true 
		Write-Debug "Path Exists: $Path" }} 
	catch { 
		Throw("Supplied Path is invalid.`n$_") } # error If invalid format
	return $Status }

$dcs = ((Get-ADForest).domains).Replace(".domain.local","")
Foreach ($dc in $dcs)
{
	If ($dc -ne "domain.local")
	{
	$allcomputers = Search-ADAccount -Server $dc -AccountInactive -TimeSpan 60.00:00:00 -ComputersOnly -SearchBase "OU=Workstations,DC=$dc,DC=domain,DC=local" 
	$Path = "OU=Disabled Computers,DC=$dc,DC=domain,DC=local"
	$OUStatus = CheckOU #Check if OU exists
	If ($OUStatus -eq $true) { Write-Host "OU:" $Path "allready exists!" }
	else { #CREATE NEW OU
		Write-Host "Creating OU:" $Path 
		$Connect = "LDAP://dc=$dc,dc=domain,dc=local"
		$AD = [adsi] $Connect
		$OU = $AD.Create("OrganizationalUnit", "OU=Disabled Computers")
		$OU.SetInfo()
		}
	ForEach ($comp in $allcomputers)
		{
		If ($comp.Enabled -eq $False) 
			{ 
			Write-Host "MOVED => Name:" $comp.Name "("$comp.DistinguishedName") LastLogonDate:" $comp.LastLogonDate
			Move-ADObject -Identity $comp -TargetPath "OU=Disabled Computers,DC=$dc,DC=domain,DC=local" 
			}
		elseif ($comp.Enabled -eq $TRUE)
			{
			Write-Host "DISABLED & MOVED => Name:" $comp.Name "("$comp.DistinguishedName")" "LastLogonDate:" $comp.LastLogonDate
			Disable-ADAccount -Identity $comp -PassThru | Move-ADObject -TargetPath "OU=Disabled Computers,DC=$dc,DC=domain,DC=local" 
			}
		}
	}
	else
	{
		$allcomputers = Search-ADAccount -AccountInactive -TimeSpan 60.00:00:00 -ComputersOnly -SearchBase "OU=Workstations,DC=domain,DC=local" 
		$Path = "OU=Disabled Computers,dc=domain,dc=local"
		$OUStatus = CheckOU #Check if OU exists
		If ($OUStatus -eq $true) 
			{ Write-Host "OU:" $Path "allready exists!" }
		else { #CREATE NEW OU
			Write-Host "Creating OU:" $Path 
			$Connect = "LDAP://dc=domain,dc=local"
			$AD = [adsi] $Connect
			$OU = $AD.Create("OrganizationalUnit", "OU=Disabled Computers")
			$OU.SetInfo() 	}
	ForEach ($comp in $allcomputers)
		{
		If ($comp.Enabled -eq $False) 
			{ 
			Write-Host "MOVED => Name:" $comp.Name "("$comp.DistinguishedName") LastLogonDate:" $comp.LastLogonDate
			Move-ADObject -Identity $comp -TargetPath "OU=Disabled Computers,DC=domain,DC=local" 
			}
		elseif ($comp.Enabled -eq $TRUE)
			{
			Write-Host "DISABLED & MOVED => Name:" $comp.Name "("$comp.DistinguishedName")" "LastLogonDate:" $comp.LastLogonDate
			Disable-ADAccount -Identity $comp -PassThru | Move-ADObject -TargetPath "OU=Disabled Computers,DC=domain,DC=local" 
			}
		}
	}
}
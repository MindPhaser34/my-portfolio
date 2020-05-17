$dcs = ((Get-ADForest).domains).Replace(".domain.local","")
$date = Get-Date
	Foreach ($dc in $dcs)
	{
	If ($dc -ne "domain.local")
	{
		$allusers = Get-ADuser -Server $dc -SearchBase "OU=Staff,DC=$dc,DC=domain,DC=local" -Properties Enabled, AccountExpirationDate, LastLogonDate -Filter *
		ForEach ($users in $allusers)
		{
		If ($users.Enabled -eq $False) 
			{
			Write-Host "Disabled Account -> Name:" $users.Name "("$users.DistinguishedName") LastLogonDate:" $users.LastLogonDate
			Move-ADObject -Identity $users -TargetPath "OU=Disabled Users,DC=$dc,DC=domain,DC=local" 
			}
		elseif (($users.Enabled -eq $TRUE) -and ($users.AccountExpirationDate -ne $NULL -and $users.AccountExpirationDate -lt $date))
			{
			Write-Host "Expired Account -> Name:"$users.Name  "("$users.DistinguishedName") AccountExpirationDate:" $users.AccountExpirationDate
			Disable-ADAccount -Identity $users -PassThru | Move-ADObject -TargetPath "OU=Disabled Users,DC=$dc,DC=domain,DC=local"
			}
		}
	}
	else
	{
		$allusers = Get-ADuser -SearchBase "OU=Staff,DC=domain,DC=local" -Properties Enabled, AccountExpirationDate, LastLogonDate -Filter *
		ForEach ($users in $allusers)
		{
		If ($users.Enabled -eq $False) 
			{
				Write-Host "Disabled Account -> Name:" $users.Name "("$users.DistinguishedName") LastLogonDate:" $users.LastLogonDate
				Move-ADObject -Identity $users -TargetPath "OU=Disabled Users,DC=domain,DC=local" 
			}
		elseif (($users.Enabled -eq $TRUE) -and ($users.AccountExpirationDate -ne $NULL -and $users.AccountExpirationDate -lt $date))
			{
				Write-Host "Expired Account -> Name:"$users.Name  "("$users.DistinguishedName") AccountExpirationDate:" $users.AccountExpirationDate
				Disable-ADAccount -Identity $users -PassThru | Move-ADObject -TargetPath "OU=Disabled Users,DC=domain,DC=local" 
			}
		}
	}
	}
Write-Host "Job is finished."
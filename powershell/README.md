# "PowerShell скрипты для системного администрирования"
Выкладываю в этот репозиторий небольшие скрипты, которые я использую в своей работе.

- Disabled_users_in_all_domains.ps1 - Скрипт пробегается по всем доменам\поддоменам в AD и ищет отключенные аккаунты пользователей, либо с аккаунты с просроченной учётной записью и перемещает их в контейней "Disabled Users"

- Disabled_computers_in_all_subdomains.ps1 - Для тех кого достало дикое количество неиспользуемых аккаунтов компьютеров в AD подойдёт данный скрипт! Он пробегается по домену\поддоменам, проверяет существование OU "Disabled computers" и если не находит его, то создаёт. Далее выбирает все учётные записи компов, которые не были активны более 2-х месяцов и перемещает в OU "Disabled computers". Функцию проверки существования OU нашёл на просторах интернета.

- List_of_enabled_comps_in_domains.ps1 - Скрипт пробегается по определённым поддоменам AD и ищет компы, которые логинились в течении месяца. Удобен при поиске рабочих компов.


# "Some powershell scripts for system administration"
- Disabled_users_in_all_domains.ps1 - This script will help you to find disabled or expired users account in Your Active Directory Forest (with all subdomains) and move it into OU for disabled users.

- Disabled_computers_in_all_subdomains.ps1 - This script will help you to find old accounts of computers in Your Active Directory Forest, and move them in OU "Disabled computers" (It'll create this OU, if it isn't).

- List_of_enabled_comps_in_domains.ps1 - Find enabled copmuters with logon date not more than 30 days.

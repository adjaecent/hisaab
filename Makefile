confgen:
	ln -s "$(CURDIR)/resources/hisaab.conf.toml" ~/hisaab.conf.toml

hdfc-cc:
	bb -m core hdfc-cc ${FILE}

hdfc-bank:
	bb -m core hdfc-bank ${FILE}

copy-toml:
	scp ~/hisaab.conf.toml ${DEST}:~

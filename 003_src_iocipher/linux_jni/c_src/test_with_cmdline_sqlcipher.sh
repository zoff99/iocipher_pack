#! /bin/bash
sqlcipher test.db <<EOM
PRAGMA key = 'this is the right password';
PRAGMA cipher_page_size = 8192;
.tables
select * from meta_data;
select * from value_data;
EOM


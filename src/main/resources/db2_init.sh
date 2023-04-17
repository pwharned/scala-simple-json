su - db2inst1 -c "source ~/.bashrc;
db2 connnect to bludb;
db2 update dbm cfg using intra_parallel yes;
db2 update dbm cfg using PYTHON_PATH $(which python);
db2set DB2_WORKLOAD=ANALYTICS;
db2 update db cfg for bludb using SHEAPTHRES_SHR 32768;
db2 update db cfg for bludb using SORTHEAP 32768;
db2stop force;
db2start;
"

echo "Creo la rete Tempest-Net per i container:" 
docker network create --subnet=172.20.0.0/16 Tempest-Net

echo "\nCreo directory per il testing ... \n" 
mkdir ./Test_Peers
mkdir ./Test_Peers/Master_Peer_Folder
mkdir ./Test_Peers/Peer_One_Folder

echo "Creo:" 
printf "Master Peer on "
osascript -e 'tell application "Terminal" to do script "echo \"MASTER PEER\n\" && docker run -i --net Tempest-Net -v /Users/beppetemp/Documents/GitHub/giuseppe-arienzo_adc_2021/Test_Peers/Master_Peer_Folder:/root/files --ip 172.20.128.0 -e MASTERIP=127.0.0.1 -e ID=0 -e WD=/root/files --name Master-Peer beppetemp/tempest_git"'
printf "Peer One on"
osascript -e 'tell application "Terminal" to do script "echo \"PEER ONE\n\" && docker run -i --net Tempest-Net -v /Users/beppetemp/Documents/GitHub/giuseppe-arienzo_adc_2021/Test_Peers/Peer_One_Folder:/root/files -e MASTERIP=172.20.128.0 -e ID=1 -e WD=/root/files --name Peer-One beppetemp/tempest_git"'

echo "\nDare invio per pulire l'ambiente e chiudere l'applicazione ..."
read enter

echo "Elimino:"
docker rm -f Master-Peer
docker rm -f Peer-One
echo "\n" 

echo "Rimuovo le directory per il testing ... \n"
rm -r ./Test_Peers

echo "Elimino la rete Tempest-Net:"
docker network rm Tempest-Net
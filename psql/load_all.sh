#!/bin/bash

declare -a experiments=("A549" "AG04449" "AG04450" "AG09309" "AG09319" "AG10803" "AoAF" "AoSMC_SFM" "BE2_C" "BJ" "CD14" "Chorion" "CLL" "CMK" "Colo829" "Fibrobl" "FibroP" "Gliobla" "GM06990" "GM12864" "GM12865" "GM12878" "GM12891" "GM12892" "GM18507" "GM19238" "GM19239" "GM19240" "H1-hESC" "H7-hESC" "H9-hESC" "HA-c" "HA-sp" "HAEpiC" "HAh" "HBMEC" "HCF" "HCFaa" "HCM" "HConF" "HCPEpiC" "HCT-116" "HEEpiC" "HeLa-S3" "HeLa-S3_IFNA" "Hepatocytes" "HepG2" "HFF" "HFF_Myc" "HGF" "HIPEpiC" "HL-60" "HMEC" "HMF" "HMVEC-dBl-Ad" "HMVEC-dBl-Neo" "HMVEC-dLy-Ad" "HMVEC-dLy-Neo" "HMVEC-dNeo" "HMVEC-LBl" "HMVEC-LLy" "HMVECdAd" "HNPCEpiC" "HPAEC" "HPAF" "HPDE6-E6E7" "HPdLF" "HPF" "HRCE" "HRE" "HRGEC" "HRPEpiC" "HSMM" "HSMMtube" "Htr8" "Huh-7" "Huh-75" "HUVEC" "HVMF" "iPS" "Jurkat" "K562" "LNCaP" "LNCaP_andro" "MCF-7" "MCF-7_hyp_lac" "Medullo" "Melano" "Myometr" "NB4" "NHA" "NHDF-Ad" "NHDF-neo" "NHEK" "NHLF" "Ntera2" "Osteobl" "PA-TU-8988T" "PANC-1" "PrEC" "ProgFib" "RPTEC" "SAEC" "SK-N-SH_RA" "SKMC" "SKNMC" "Stellate" "Th1" "Th2" "Urothelia_UT189" "WI-38" "WI-38-TAM")

#declare explength=${#experiments[@]}
explength=${#experiments[@]}

for (( i=1; i<${explength}+1; i++ )); 
do
	echo "Working on experiment" $i " / " ${explength} " : " ${experiments[$i-1]}
	column=$((i + 2))
	if [ $i -eq 1 ]; then
		echo python load_annotations.py $1 ${column} $2 noskip
		python load_annotations.py $1 ${column} $2 noskip
	else
		echo python load_annotations.py $1 ${column} $2 skip
		python load_annotations.py $1 ${column} $2 skip
	fi
done

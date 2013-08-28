for dir in /home/jdoerrie/zbl_cc/MSC*; do
    cd $dir
    matlab -nosplash -nodisplay -nojvm -nodesktop < ../myScript.m
done

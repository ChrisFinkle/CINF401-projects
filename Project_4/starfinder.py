from pyspark import SparkContext, SparkConf
import cv2
import numpy as np

def getStarAreas(filename):
    img = cv2.imread("/bigdata/data/pan-starrs1/"+filename,0)
    out = []
    if img is not None:
        retval,img = cv2.threshold(img,200,255,cv2.THRESH_BINARY)
        kernel = np.ones((4,4),np.uint8)
        img = cv2.erode(img,kernel,iterations=2)
        img = cv2.dilate(img,kernel,iterations=1)
        contours,_ = cv2.findContours(img,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)
        for s in range(len(contours)):
            cnt = contours[s]
            x,y,w,h = cv2.boundingRect(cnt)
            ar = float(w)/h
            area = cv2.contourArea(contours[s]) if ar > 0.5 and ar < 2 else -1
            data = (area,filename,x+w/2,y+h/2)
            out.append(data)
    return out

if __name__ == "__main__":
    conf = SparkConf().setAppName("StarFinder")
    sc = SparkContext(conf=conf)
    starfiles = sc.textFile("file:///home/cfinkle/cinf401-project-4/starfilenames.txt")
    stardata = starfiles.flatMap(getStarAreas)
    tophundred = stardata.takeOrdered(100,key = lambda x: -x[0])
    f = open('/home/cfinkle/cinf401-project-4/results.txt','w')
    ra_dec = open('/bigdata/data/pan-starrs1/radec.csv','r')
    rds = {line.split(',')[0]+".png" : line.strip().split(',')[1:3] for line in ra_dec}
    ra_dec.close()
    th = []
    img_width = 6291
    img_height = 6247
    for s in tophundred:
        fn = s[1].encode('ascii','ignore')
        ra = float(rds[fn][0])+(0.25/3600)*((img_width-s[2])-img_width/2.0)
        ra += 0 if ra > 0 else 360
        dec = float(rds[fn][1])+(0.25/3600)*((img_height-s[3])-img_height/2.0)
        tup = (s[0],ra,dec)
        th.append(tup)
    f.write('\n'.join('%s,%s,%s' % x for x in th))
    f.close()

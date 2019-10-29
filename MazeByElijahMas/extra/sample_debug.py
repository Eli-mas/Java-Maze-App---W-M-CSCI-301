import numpy as np
import matplotlib.pyplot as plt

def mazeplot():
	for l in walls.split('\n'):
	    if(l.strip()):
	    	l=l.replace(':','').split()
	    	x,y=map(int,l[:2])
	    	w,s,e,n = (True if d=='true' else False for d in l[2:])
	    	if w: _=plt.plot([x-.5,x-.5],[y-.5,y+.5],c='k')
	    	if s: _=plt.plot([x-.5,x+.5],[y+.5,y+.5],c='k')
	    	if e: _=plt.plot([x+.5,x+.5],[y-.5,y+.5],c='k')
	    	if n: _=plt.plot([x-.5,x+.5],[y-.5,y-.5],c='k')
	
	plt.text(positions[0,0],positions[0,1],1)
	count=2
	for i in range(1,len(positions)):
		if not np.array_equal(positions[i], positions[i-1]):
			plt.text(positions[i,0],positions[i,1],count)
			count+=1
	
	
	plt.show()

walls="""0 0 : true false false true 
0 1 : true false true false 
0 2 : true true false false 
0 3 : true false false true 
0 4 : true true false false 
0 5 : false false true true 
0 6 : true true false false 
0 7 : true false false true 
0 8 : true false true false 
0 9 : true false true false 
0 10 : true false true false 
0 11 : true true false false 
1 0 : false false false true 
1 1 : true true false false 
1 2 : false false true true 
1 3 : false true true false 
1 4 : false false true true 
1 5 : true false true false 
1 6 : false true true false 
1 7 : false true false true 
1 8 : true true false true 
1 9 : true false false true 
1 10 : true false true false 
1 11 : false true true false 
2 0 : false true true true 
2 1 : false false false true 
2 2 : true false true false 
2 3 : true false true false 
2 4 : true false true false 
2 5 : true true false false 
2 6 : true false false true 
2 7 : false true true false 
2 8 : false true false true 
2 9 : false true false true 
2 10 : true false false true 
2 11 : true true false false 
3 0 : true true false true 
3 1 : false false true true 
3 2 : true false true false 
3 3 : true false true false 
3 4 : true true true false 
3 5 : false false true true 
3 6 : false false true false 
3 7 : true false true false 
3 8 : false true true false 
3 9 : false false true true 
3 10 : false true false false 
3 11 : false true false true 
4 0 : false false true true 
4 1 : true false true false 
4 2 : true true false false 
4 3 : true false false true 
4 4 : true false false false 
4 5 : true false false false 
4 6 : true false false false 
4 7 : true false false false 
4 8 : true false false false 
4 9 : true true false false 
4 10 : false true false true 
4 11 : false true false true 
5 0 : true false false true 
5 1 : true false true false 
5 2 : false true true false 
5 3 : false false false true 
5 4 : false false false false 
5 5 : false false false false 
5 6 : false false false false 
5 7 : false false false false 
5 8 : false false false false 
5 9 : false true false false 
5 10 : false true true true 
5 11 : false true false true 
6 0 : false false true true 
6 1 : true true false false 
6 2 : true true false true 
6 3 : false false false true 
6 4 : false false false false 
6 5 : false false false false 
6 6 : false false false false 
6 7 : false false false false 
6 8 : false false false false 
6 9 : false true false false 
6 10 : true false false true 
6 11 : false true true false 
7 0 : true false false true 
7 1 : false true true false 
7 2 : false true false true 
7 3 : false false false true 
7 4 : false false false false 
7 5 : false false false false 
7 6 : false false false false 
7 7 : false false false false 
7 8 : false false false false 
7 9 : false true false false 
7 10 : false false false true 
7 11 : true true false false 
8 0 : false true false true 
8 1 : true false false true 
8 2 : false true true false 
8 3 : false false false true 
8 4 : false false false false 
8 5 : false false false false 
8 6 : false false false false 
8 7 : false false false false 
8 8 : false false false false 
8 9 : false false false false 
8 10 : false true false false 
8 11 : false true false true 
9 0 : false true false true 
9 1 : false false false true 
9 2 : true true false false 
9 3 : false false false true 
9 4 : false false false false 
9 5 : false false false false 
9 6 : false false false false 
9 7 : false false false false 
9 8 : false false false false 
9 9 : false true false false 
9 10 : false true false true 
9 11 : false true false true 
10 0 : false true false true 
10 1 : false true true true 
10 2 : false true false true 
10 3 : false false true true 
10 4 : false false false false 
10 5 : false false true false 
10 6 : false false true false 
10 7 : false false true false 
10 8 : false false false false 
10 9 : false true true false 
10 10 : false true true true 
10 11 : false true false true 
11 0 : false false true true 
11 1 : true false true false 
11 2 : false false true false 
11 3 : true false true false 
11 4 : false true true false 
11 5 : true false true true 
11 6 : true false true false 
11 7 : true false true false 
11 8 : false false true false 
11 9 : true false true false 
11 10 : true false true false 
11 11 : false true true false 
"""
positions=np.array([[3, 0], [4, 0], [4, 0], [4, 1], [4, 1], [4, 2], [4, 2], [5, 2], [5, 2], 
[5, 1], [5, 1], [5, 0], [5, 0], [6, 0], [6, 0], [6, 1], [6, 1], [7, 1], [7, 1], 
[7, 0], [7, 0], [8, 0], [8, 0], [9, 0], [9, 0], [10, 0], [10, 0], [11, 0], [11, 0], 
[11, 1], [11, 1], [11, 2], [11, 2], [10, 2], [10, 2], [9, 2], [9, 2], [9, 1], [9, 1], 
[10, 1], [10, 1], [9, 1], [9, 1], [8, 1], [8, 1], [8, 2], [8, 2], [7, 2], [7, 2], 
[6, 2], [6, 2], [7, 2], [7, 2], [6, 2], [6, 2], [7, 2], [7, 2], [8, 2], [8, 2], 
[8, 1], [8, 1], [9, 1], [9, 1], [9, 2], [9, 2], [10, 2], [10, 2], [11, 2], [11, 2], 
[11, 3], [11, 3], [11, 4], [11, 4], [10, 4], [10, 4], [10, 5], [10, 6], [10, 7], [10, 8], 
[10, 9], [9, 9], [8, 9], [7, 9], [6, 9], [5, 9], [4, 9], [4, 8], [4, 7], [4, 6], 
[4, 5], [4, 4], [4, 3], [5, 3], [6, 3], [7, 3], [8, 3], [9, 3], [10, 3], [10, 4], 
[9, 4], [8, 4], [8, 3], [8, 3], ])
mazeplot()

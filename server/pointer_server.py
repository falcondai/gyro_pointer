import socket
import time
import struct
import win32api
import win32con

def main():
	# TODO support bluetooth socket
	srv = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	# srv.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
	srv.bind(('', 50003))

	print 'Pointer server established.'

	# latest timestamp in received packet
	ct = 0
	# local time
	lt = time.clock()
	# reception/update frequency
	uf = 0.0
	# screen resolution
	W = win32api.GetSystemMetrics(0)
	H = win32api.GetSystemMetrics(1)
	# TODO make configurations adjustable
	while True:
		data, addr = srv.recvfrom(20)
		# print 'From', addr, ':', data

		print 'From', addr, ':', struct.unpack('qfff', data)
		t, x, y, z = struct.unpack('qfff', data)

		if t > ct:
			# the packet is up to date
			ct = t
			
			rt = time.clock()
			uf = 1.0 / (rt - lt)
			lt = rt
			# print 'Update frequency:', uf
			print x, y, z
			
			# convert the raw phone rot state to cursor position
			# TODO use inertia and resistance to smooth the motion
			
			rot = rv_to_rot(x, y, z)
			ya = [[0.0],[1.0],[0.0]]
			xa = [[1.0],[0.0],[0.0]]
			ty = mmulti(rot, ya)
			tx = mmulti(rot, xa)
			print ty
			print tx
			cx = ty[0][0] * W + W / 2.0
			cy = -ty[2][0] * H + H / 2.0
			print 'cursor position:', cx, cy
			win32api.SetCursorPos((int(cx), int(cy)))

def mmulti(a, b):
	if len(a[0]) != len(b):
		return None
	
	c = []
	m = len(a)
	n = len(b[0])
	l = len(a[0])
	for i in range(m):
		r = []
		for j in range(n):
			t = 0.0
			for k in range(l):
				t += a[i][k] * b[k][j]
			r.append(t)
		c.append(r)
	return c
			
def mtranspose(a):
	b = []
	n = len(a)
	m = len(a[0])
	for i in range(m):
		r = []
		for j in range(n):
			r.append(a[j][i])
		b.append(r)
	return b

def rv_to_rot(x, y, z):
	xx = x**2
	yy = y**2
	zz = z**2
	w = 1.0 - xx - yy - zz
	xy = x*y
	zw = z*w
	xz = x*z
	yw = y*w
	yz = y*z
	xw = x*w
	
	rot = [
		[1.0-2*yy-2*zz, 2*xy-2*zw, 2*xz+2*yw],
		[2*xy+2*zw, 1.0-2*xx-2*zz, 2*yz-2*xw],
		[2*xz-2*yw, 2*yz+2*xw, 1-2*xx-2*yy]
	]
	return rot
	
if __name__ == '__main__':
	main()
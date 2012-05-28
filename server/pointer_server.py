import socket
import time
import struct
import math

# TODO wrap OS dependent functions into a class
import win32api
import win32con

def main():
	# TODO support bluetooth socket
	srv = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	# srv.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
	srv.bind(('', 50003))

	print 'Pointer server established at', socket.gethostbyname(socket.gethostname())
	
	# screen resolution
	W = win32api.GetSystemMetrics(0)
	H = win32api.GetSystemMetrics(1)
	
	# TODO make configurations adjustable
	HR = 0.3
	VR = float(H) / float(W) * HR
	# low-pass filter parameters
	ALPHA = 0.6
	
	# initial state
	# facing the screen the RH coordinate system where x points to the right
	# and y points into the screen.
	orientation_reset = True;
	# latest timestamp in received packet
	ct = 0
	# local time
	lt = time.clock()
	# reception/update frequency
	uf = 0.0
	# rotation vector
	rx = 0.0
	ry = 0.0
	rz = 0.0

	# constants
	ya = [[0.0],[1.0],[0.0]]
	xa = [[1.0],[0.0],[0.0]]
	
	# handle messages
	while True:
		data, addr = srv.recvfrom(20)
		# print 'From', addr, ':', data

		# print 'From', addr, ':', struct.unpack('qfff', data)
		t, x, y, z = struct.unpack('qfff', data)

		if t > ct:
			# the packet is up to date
			ct = t
			
			rt = time.clock()
			uf = 1.0 / (rt - lt)
			lt = rt
			# print 'Update frequency:', uf
			# print x, y, z
			
			# detect message types
			if x >= -1.0 and x <= 1.0:
				# rotation vector state message
				
				# convert the raw phone rot state to cursor position
				# TODO use inertia and resistance to smooth the motion
				
				if orientation_reset:
					rot = rv_to_rot(x, y, z)
					rx = 0.0
					ry = 0.0
					rz = 0.0
					SCREEN_ORIENTATION = mtranspose(rot)
					orientation_reset = False
				else:
					# low-pass filter the data
					rx = (1.0-ALPHA)*rx + ALPHA*x
					ry = (1.0-ALPHA)*ry + ALPHA*y
					rz = (1.0-ALPHA)*rz + ALPHA*z
					rot = rv_to_rot(rx, ry, rz)
					
					ty = mmultiply(SCREEN_ORIENTATION, mmultiply(rot, ya))
					if ty[1][0] > 0.0:
						# facing the screen
						cx = ty[0][0] * W / 2.0 / HR + W / 2.0
						cy = -ty[2][0] * H / 2.0 / VR + H / 2.0
						# print 'cursor position:', cx, cy, 'update frequency:', uf
						# win32api.SetCursorPos((int(cx), int(cy)))
						win32api.mouse_event(win32con.MOUSEEVENTF_MOVE|win32con.MOUSEEVENTF_ABSOLUTE, int(cx/W*65535.0), int(cy/H*65535.0))
			
			elif data[8:12] == '\x00\x00\x00\x40':
				# mouse button event message
				
				print data[12:20].split('\\')
				button, state = struct.unpack('ii', data[12:20])
				print 'mouse button event:', button, state
				
				handle_mouse_button_event(button, state)
			
			elif data[8:12] == '\x00\x00\x40\x40':
				# orientation reset message
				
				print 'reset orientation'
				orientation_reset = True
				
				
def handle_mouse_button_event(button, state):
	if button == 0:
		if state > 0:
			win32api.mouse_event(win32con.MOUSEEVENTF_LEFTUP, 0, 0)
		else:
			win32api.mouse_event(win32con.MOUSEEVENTF_LEFTDOWN, 0, 0)
	elif button == 1:
		if state > 0:
			win32api.mouse_event(win32con.MOUSEEVENTF_MIDDLEUP, 0, 0)
		else:
			win32api.mouse_event(win32con.MOUSEEVENTF_MIDDLEDOWN, 0, 0)
	elif button == 2:
		if state > 0:
			win32api.mouse_event(win32con.MOUSEEVENTF_RIGHTUP, 0, 0)
		else:
			win32api.mouse_event(win32con.MOUSEEVENTF_RIGHTDOWN, 0, 0)

def mmultiply(a, b):
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

def uqmultiply(a, b):
	# unit quaternion multiplication
	x1, y1, z1 = a
	x2, y2, z2 = b
	w1 = math.sqrt(1.0 - x1**2 - y1**2 - z1**2)
	w2 = math.sqrt(1.0 - x2**2 - y2**2 - z2**2)
	
	return (w1*x2+w2*x1+y1*z2-z1*y2, w1*y2+w2*y1+z1*x2-x1*z2, w1*z2+w2*z1+x1*y2-y1*x2)
	
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

def vdot(a, b):
	if len(a) != len(b):
		return None
		
	s = 0.0
	for i in range(len(a)):
		s += a[i][0] * b[i][0]
	return s
	
def v3cross(a, b):
	ax = a[0][0]
	ay = a[1][0]
	az = a[2][0]
	bx = b[0][0]
	by = b[1][0]
	bz = b[2][0]
	return [[ay*bz-az*by], [az*bx-ax*bz], [ax*by-ay*bx]]

def rv(x, y, z, theta):
	return [[x * math.sin(theta/2.0)], [y * math.sin(theta/2.0)], [z * math.sin(theta/2.0)]]
	
if __name__ == '__main__':
	main()
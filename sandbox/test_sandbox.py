import json, subprocess, sys, time, urllib.request

p=subprocess.Popen([sys.executable, 'server.py'], cwd='sandbox')
try:
    time.sleep(0.4)
    base='http://127.0.0.1:8080'
    def req(path, method='GET', body=None):
        data=None if body is None else json.dumps(body).encode()
        r=urllib.request.urlopen(urllib.request.Request(base+path,data=data,method=method,headers={'Content-Type':'application/json'}), timeout=3)
        return r.status, r.read().decode()
    assert req('/wp-json/wc/v3/system_status')[0] == 200
    status, products=req('/wp-json/wc/v3/products')
    assert status == 200 and json.loads(products)[0]['id'] == 101
    status, created=req('/wp-json/wc/v3/products', 'POST', {'name':'Recorder Test','price':'9.99'})
    assert status == 201 and json.loads(created)['id'] == 103
    status, updated=req('/wp-json/wc/v3/products/103', 'PUT', {'price':'10.00'})
    assert status == 200 and json.loads(updated)['price'] == '10.00'
    status, deleted=req('/wp-json/wc/v3/products/103', 'DELETE')
    assert status == 200 and json.loads(deleted)['deleted'] is True
    print('sandbox smoke test: PASS')
finally:
    p.terminate(); p.wait(timeout=3)

#!/usr/bin/env python3
"""Zero-dependency WooCommerce REST sandbox + request recorder for WooGit."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
from pathlib import Path
import json, threading, time, uuid, os

ROOT = Path(__file__).resolve().parent
STATE_FILE = ROOT / "state.json"
LOCK = threading.Lock()

DEFAULT_STATE = {
    "products": [
        {"id": 101, "name": "Sandbox T-Shirt", "slug": "sandbox-t-shirt", "type": "simple", "status": "publish", "price": "25.00", "regular_price": "25.00", "stock_quantity": 42, "stock_status": "instock", "date_modified_gmt": "2026-08-27T00:00:00"},
        {"id": 102, "name": "Sandbox Mug", "slug": "sandbox-mug", "type": "simple", "status": "publish", "price": "12.50", "regular_price": "12.50", "stock_quantity": 18, "stock_status": "instock", "date_modified_gmt": "2026-08-27T00:00:00"}
    ],
    "orders": [
        {"id": 201, "status": "processing", "currency": "USD", "total": "37.50", "date_created_gmt": "2026-08-27T08:00:00", "date_modified_gmt": "2026-08-27T08:00:00", "billing": {"first_name": "Test", "last_name": "Customer"}, "line_items": [{"id": 1, "name": "Sandbox T-Shirt", "product_id": 101, "quantity": 1, "total": "25.00"}]}
    ],
    "customers": [],
    "categories": [{"id": 301, "name": "Sandbox", "slug": "sandbox", "count": 2}],
    "attributes": [],
    "terms": {},
    "variations": {},
    "next_ids": {"products": 103, "orders": 202, "customers": 401, "categories": 302, "attributes": 501, "terms": 601, "variations": 701},
    "error_mode": None,
    "requests": []
}

def load_state():
    if not STATE_FILE.exists():
        save_state(DEFAULT_STATE)
    try:
        return json.loads(STATE_FILE.read_text())
    except Exception:
        save_state(DEFAULT_STATE)
        return json.loads(json.dumps(DEFAULT_STATE))

def save_state(state):
    STATE_FILE.write_text(json.dumps(state, ensure_ascii=False, indent=2))


def now(): return time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())

def response_body(obj): return json.dumps(obj, ensure_ascii=False).encode('utf-8')

class Handler(BaseHTTPRequestHandler):
    server_version = 'WooGitSandbox/1.0'

    def log_message(self, fmt, *args): pass

    def _read_body(self):
        length = int(self.headers.get('Content-Length', '0') or 0)
        return self.rfile.read(length) if length else b''

    def _record(self, method, path, query, body, status, response, started):
        with LOCK:
            state = load_state()
            state['requests'].append({
                'id': str(uuid.uuid4()), 'timestamp': now(), 'method': method,
                'path': path, 'query': query, 'request_headers': {k: v for k, v in self.headers.items()},
                'request_body': body.decode('utf-8', errors='replace'), 'status': status,
                'response_body': response.decode('utf-8', errors='replace'),
                'duration_ms': round((time.time()-started)*1000, 2)
            })
            state['requests'] = state['requests'][-500:]
            save_state(state)

    def _send(self, status, obj, method='GET', started=None):
        body = response_body(obj)
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('X-WooGit-Sandbox', 'true')
        self.send_header('Content-Length', str(len(body)) if method != 'HEAD' else '0')
        self.end_headers()
        if method != 'HEAD': self.wfile.write(body)
        if started is not None: self._record(method, self.path.split('?',1)[0], parse_qs(urlparse(self.path).query), self._request_body, status, body, started)

    def _error_mode(self):
        with LOCK: return load_state().get('error_mode')

    def _route(self, method):
        started = time.time(); self._request_body = self._read_body()
        parsed = urlparse(self.path); path = parsed.path; q = parse_qs(parsed.query)
        mode = self._error_mode()
        if mode:
            if mode == 'timeout': time.sleep(30); self._send(504, {'code':'sandbox_timeout','message':'Injected timeout'}, method, started); return
            status = int(mode) if mode.isdigit() else {'malformed': 200}.get(mode, 500)
            if mode == 'malformed':
                raw = b'{malformed-json'
                self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(raw))); self.end_headers(); self.wfile.write(raw); self._record(method,path,q,self._request_body,200,raw,started); return
            self._send(status, {'code': 'sandbox_injected_error', 'message': f'Injected HTTP {status}'}, method, started); return
        if path == '/wp-json/wc/v3/system_status' and method == 'GET':
            self._send(200, {'environment': {'version':'9.9.0-sandbox','wp_version':'6.8.2','php_version':'8.3'}, 'settings': {}, 'sandbox': True}, method, started); return
        prefix = '/wp-json/wc/v3/'
        if path.startswith(prefix):
            self._wc(method, path[len(prefix):].strip('/'), q, started); return
        if path == '/__sandbox__/state' and method == 'GET':
            with LOCK: self._send(200, load_state(), method, started)
            return
        if path == '/__sandbox__/requests' and method == 'GET':
            with LOCK: self._send(200, load_state()['requests'], method, started)
            return
        if path == '/__sandbox__/reset' and method == 'POST':
            with LOCK: save_state(json.loads(json.dumps(DEFAULT_STATE))); self._send(200, {'ok':True}, method, started)
            return
        if path == '/__sandbox__/error-mode' and method in ('GET','POST'):
            with LOCK:
                state=load_state()
                if method == 'POST': state['error_mode']=(json.loads(self._request_body or b'{}').get('mode')); save_state(state)
                self._send(200, {'mode': state.get('error_mode')}, method, started)
            return
        self._send(404, {'code':'rest_no_route','message':'No route was found matching the URL and request method.'}, method, started)

    def _wc(self, method, resource, q, started):
        parts=resource.split('/') if resource else []
        collection=parts[0] if parts else ''
        with LOCK: state=load_state()
        key={'products':'products','orders':'orders','customers':'customers','categories':'categories','attributes':'attributes'}.get(collection)
        if collection == 'products' and len(parts) >= 2 and parts[1].isdigit() and len(parts) >= 3 and parts[2] == 'variations':
            return self._variations(method, state, int(parts[1]), parts[3:], started)
        if collection == 'products' and len(parts) >= 3 and parts[2] == 'attributes':
            return self._send(200, [], method, started)
        if collection == 'products' and len(parts) >= 3 and parts[2] == 'categories': return self._send(200, [], method, started)
        if not key: return self._send(404, {'code':'woocommerce_rest_invalid_endpoint','message':'Unknown sandbox endpoint.'}, method, started)
        if len(parts)==1:
            if method == 'GET':
                items=state[key]
                search=(q.get('search') or [''])[0].lower()
                status=(q.get('status') or [''])[0]
                if search: items=[x for x in items if search in str(x.get('name','')).lower() or search in str(x.get('id','')).lower()]
                if status: items=[x for x in items if x.get('status') == status]
                page=max(1,int((q.get('page') or ['1'])[0])); per=min(100,max(1,int((q.get('per_page') or ['20'])[0]))); start=(page-1)*per
                return self._send(200, items[start:start+per], method, started)
            if method == 'POST':
                try: body=json.loads(self._request_body or b'{}')
                except Exception: return self._send(400, {'code':'rest_invalid_json','message':'Invalid JSON'}, method, started)
                body['id']=state['next_ids'][key]; state['next_ids'][key]+=1; body.setdefault('date_modified_gmt',now()); state[key].append(body); save_state(state); return self._send(201,body,method,started)
        if len(parts)>=2 and parts[1].isdigit():
            ident=int(parts[1]); item=next((x for x in state[key] if x.get('id')==ident),None)
            if item is None: return self._send(404, {'code':'woocommerce_rest_invalid_id','message':'Resource not found.'}, method, started)
            if method=='GET': return self._send(200,item,method,started)
            if method in ('PUT','PATCH'):
                try: body=json.loads(self._request_body or b'{}')
                except Exception: return self._send(400, {'code':'rest_invalid_json','message':'Invalid JSON'}, method, started)
                item.update(body); item['id']=ident; item['date_modified_gmt']=now(); save_state(state); return self._send(200,item,method,started)
            if method=='DELETE': state[key].remove(item); save_state(state); return self._send(200,{**item,'deleted':True},method,started)
        return self._send(404, {'code':'rest_no_route','message':'Unknown sandbox route.'}, method, started)

    def _variations(self, method, state, product_id, tail, started):
        arr=state.setdefault('variations',{}).setdefault(str(product_id),[])
        if not tail:
            if method=='GET': return self._send(200,arr,method,started)
            if method=='POST':
                body=json.loads(self._request_body or b'{}'); body['id']=state['next_ids']['variations']; state['next_ids']['variations']+=1; arr.append(body); save_state(state); return self._send(201,body,method,started)
        if tail[0].isdigit():
            item=next((x for x in arr if x.get('id')==int(tail[0])),None)
            if item is None: return self._send(404, {'code':'woocommerce_rest_invalid_id','message':'Variation not found.'},method,started)
            if method=='GET': return self._send(200,item,method,started)
            if method in ('PUT','PATCH'): item.update(json.loads(self._request_body or b'{}')); save_state(state); return self._send(200,item,method,started)
            if method=='DELETE': arr.remove(item); save_state(state); return self._send(200,{**item,'deleted':True},method,started)
        return self._send(404, {'code':'rest_no_route','message':'Unknown variation route.'},method,started)

    def do_GET(self): self._route('GET')
    def do_POST(self): self._route('POST')
    def do_PUT(self): self._route('PUT')
    def do_PATCH(self): self._route('PATCH')
    def do_DELETE(self): self._route('DELETE')

if __name__ == '__main__':
    port=int(os.environ.get('PORT','8080'))
    print(f'WooGit API Sandbox listening on http://0.0.0.0:{port}')
    print(f'Recorder: http://127.0.0.1:{port}/__sandbox__/requests')
    ThreadingHTTPServer(('0.0.0.0',port),Handler).serve_forever()

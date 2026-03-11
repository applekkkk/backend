const API_BASE = '/api';
let currentUser = null;
let currentView = 'login';

const api = {
    login: (account, password) => fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({account, password})
    }).then(r => r.json()),

    checkIn: (userId) => fetch(`${API_BASE}/auth/check-in?userId=${userId}`, {method: 'POST'}).then(r => r.json()),

    getProducts: () => fetch(`${API_BASE}/products`).then(r => r.json()),
    getProduct: (id) => fetch(`${API_BASE}/products/${id}`).then(r => r.json()),
    uploadProduct: (product) => fetch(`${API_BASE}/products`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(product)
    }).then(r => r.json()),

    getOrders: (userId) => fetch(`${API_BASE}/orders?userId=${userId}`).then(r => r.json()),
    createOrder: (order) => fetch(`${API_BASE}/orders`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(order)
    }).then(r => r.json()),

    getRequests: () => fetch(`${API_BASE}/requests`).then(r => r.json()),
    publishRequest: (request) => fetch(`${API_BASE}/requests`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(request)
    }).then(r => r.json()),
    acceptRequest: (id, acceptorId, acceptorName) => fetch(`${API_BASE}/requests/${id}/accept?acceptorId=${acceptorId}&acceptorName=${acceptorName}`, {
        method: 'POST'
    }).then(r => r.json())
};

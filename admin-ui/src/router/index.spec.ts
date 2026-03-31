describe('router/index', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  it('registers login, forbidden, and app shell routes', async () => {
    const captured: { routes?: unknown[]; guard?: (to: any) => any } = {}

    vi.doMock('vue-router', () => ({
      createRouter: vi.fn((options) => {
        captured.routes = options.routes
        return {
          beforeEach: (guard: (to: any) => any) => {
            captured.guard = guard
          }
        }
      }),
      createWebHistory: vi.fn(() => ({}))
    }))

    vi.doMock('@/router/access', () => ({
      appRouteChildren: [{ path: '', name: 'dashboard' }],
      canAccessRoute: vi.fn(() => true)
    }))

    vi.doMock('@/store/auth', () => ({
      useAuthStore: () => ({
        state: {
          session: null,
          currentUser: null
        },
        ensureCurrentUser: vi.fn()
      })
    }))

    await import('@/router/index')

    const routes = captured.routes as Array<{ path: string; meta?: Record<string, unknown>; children?: unknown[] }>
    expect(routes.map((route) => route.path)).toEqual(['/login', '/403', '/'])
    expect(routes[0].meta?.public).toBe(true)
    expect(routes[1].meta?.requiresAuth).toBe(true)
    expect(routes[2].children).toHaveLength(1)
    expect(typeof captured.guard).toBe('function')
  })

  it('redirects unauthenticated users to login with the target path', async () => {
    const captured: { guard?: (to: any) => any } = {}

    vi.doMock('vue-router', () => ({
      createRouter: vi.fn(() => ({
        beforeEach: (guard: (to: any) => any) => {
          captured.guard = guard
        }
      })),
      createWebHistory: vi.fn(() => ({}))
    }))

    vi.doMock('@/router/access', () => ({
      appRouteChildren: [],
      canAccessRoute: vi.fn(() => true)
    }))

    vi.doMock('@/store/auth', () => ({
      useAuthStore: () => ({
        state: {
          session: null,
          currentUser: null
        },
        ensureCurrentUser: vi.fn()
      })
    }))

    await import('@/router/index')

    const result = await captured.guard?.({
      fullPath: '/devices',
      meta: {}
    })

    expect(result).toEqual({
      path: '/login',
      query: {
        redirect: '/devices'
      }
    })
  })

  it('allows public routes for guests but redirects signed-in users away from login', async () => {
    const captured: { guard?: (to: any) => any } = {}
    const authStore = {
      state: {
        session: {
          token: 'token',
          userInfo: {
            roleCode: 'ADMIN'
          }
        },
        currentUser: {
          roleCode: 'ADMIN'
        }
      },
      ensureCurrentUser: vi.fn()
    }

    vi.doMock('vue-router', () => ({
      createRouter: vi.fn(() => ({
        beforeEach: (guard: (to: any) => any) => {
          captured.guard = guard
        }
      })),
      createWebHistory: vi.fn(() => ({}))
    }))

    vi.doMock('@/router/access', () => ({
      appRouteChildren: [],
      canAccessRoute: vi.fn(() => true)
    }))

    vi.doMock('@/store/auth', () => ({
      useAuthStore: () => authStore
    }))

    await import('@/router/index')

    const result = await captured.guard?.({
      fullPath: '/login',
      meta: {
        public: true
      }
    })

    expect(result).toBe('/')
  })

  it('blocks first-login users from non-whitelisted pages and sends forbidden roles to /403', async () => {
    const captured: { guard?: (to: any) => any } = {}
    const canAccessRouteMock = vi.fn(() => false)
    const authStore = {
      state: {
        session: {
          token: 'token',
          firstLoginRequired: true,
          userInfo: {
            roleCode: 'TEACHER'
          }
        },
        currentUser: {
          roleCode: 'TEACHER'
        }
      },
      ensureCurrentUser: vi.fn()
    }

    vi.doMock('vue-router', () => ({
      createRouter: vi.fn(() => ({
        beforeEach: (guard: (to: any) => any) => {
          captured.guard = guard
        }
      })),
      createWebHistory: vi.fn(() => ({}))
    }))

    vi.doMock('@/router/access', () => ({
      appRouteChildren: [],
      canAccessRoute: canAccessRouteMock
    }))

    vi.doMock('@/store/auth', () => ({
      useAuthStore: () => authStore
    }))

    await import('@/router/index')

    const firstLoginRedirect = await captured.guard?.({
      fullPath: '/users',
      meta: {}
    })

    expect(firstLoginRedirect).toBe('/')
    expect(authStore.ensureCurrentUser).toHaveBeenCalled()

    authStore.state.session.firstLoginRequired = false

    const forbiddenRedirect = await captured.guard?.({
      fullPath: '/statistics',
      meta: {
        requiresAuth: true
      }
    })

    expect(canAccessRouteMock).toHaveBeenCalled()
    expect(forbiddenRedirect).toBe('/403')
  })
})

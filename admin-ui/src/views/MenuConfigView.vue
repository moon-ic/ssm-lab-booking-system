<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMenuConfigs, listIcons, updateMenuConfig } from '@/api/menu-config'
import { listPermissions } from '@/api/roles'
import type { MenuConfigItem, UpdateMenuConfigPayload } from '@/types/menu-config'
import type { PermissionItem } from '@/types/role'

const loading = ref(false)
const detailLoading = ref(false)
const saveDialogVisible = ref(false)
const saveFormRef = ref()

const menus = ref<MenuConfigItem[]>([])
const icons = ref<string[]>([])
const permissions = ref<PermissionItem[]>([])

const selectedMenuId = ref<number>()
const selectedMenu = ref<MenuConfigItem | null>(null)

const saveForm = reactive<UpdateMenuConfigPayload & { menuId?: number }>({
  menuId: undefined,
  menuName: '',
  path: '',
  icon: '',
  permissionCode: ''
})

const saveRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  icon: [{ required: true, message: '请选择图标', trigger: 'change' }],
  permissionCode: [{ required: true, message: '请选择权限编码', trigger: 'change' }]
}

const permissionOptions = computed(() => permissions.value.filter((item) => item.type === 'ACTION'))

const summaryCards = computed(() => [
  {
    title: '菜单',
    value: menus.value.length,
    description: '当前已登记的可配置菜单项'
  },
  {
    title: '图标',
    value: icons.value.length,
    description: '可用于菜单配置的内置图标名称'
  },
  {
    title: '权限编码',
    value: permissionOptions.value.length,
    description: '可与菜单项绑定的操作权限编码'
  }
])

async function loadInitialData() {
  loading.value = true

  try {
    const [menuList, iconList, permissionList] = await Promise.all([
      listMenuConfigs(),
      listIcons(),
      listPermissions('ACTION')
    ])

    menus.value = menuList
    icons.value = iconList
    permissions.value = permissionList

    if (menuList.length > 0) {
      await selectMenu(menuList[0].menuId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载菜单配置失败')
  } finally {
    loading.value = false
  }
}

async function selectMenu(menuId: number) {
  selectedMenuId.value = menuId
  detailLoading.value = true

  try {
    selectedMenu.value = menus.value.find((item) => item.menuId === menuId) ?? null
  } finally {
    detailLoading.value = false
  }
}

function openEditDialog(menu: MenuConfigItem) {
  saveForm.menuId = menu.menuId
  saveForm.menuName = menu.menuName
  saveForm.path = menu.path
  saveForm.icon = menu.icon
  saveForm.permissionCode = menu.permissionCode
  saveDialogVisible.value = true
}

async function submitSave() {
  await saveFormRef.value?.validate()
  if (!saveForm.menuId) {
    return
  }

  try {
    const payload: UpdateMenuConfigPayload = {
      menuName: saveForm.menuName,
      path: saveForm.path,
      icon: saveForm.icon,
      permissionCode: saveForm.permissionCode
    }

    const updated = await updateMenuConfig(saveForm.menuId, payload)
    menus.value = menus.value.map((item) => (item.menuId === updated.menuId ? updated : item))
    selectedMenu.value = updated
    saveDialogVisible.value = false
    ElMessage.success('菜单配置已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

onMounted(() => {
  void loadInitialData()
})
</script>

<template>
  <div class="menu-config-page">
    <section class="hero-card">
      <span class="eyebrow">菜单模块</span>
      <h2>菜单与图标配置</h2>
      <p>
        该页面面向超级管理员，用于查看已登记菜单、选择内置图标，并维护菜单标题、路径与权限编码。
      </p>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.title" class="summary-card">
        <span>{{ card.title }}</span>
        <strong>{{ card.value }}</strong>
        <p>{{ card.description }}</p>
      </article>
    </section>

    <section class="workspace-grid" v-loading="loading">
      <article class="menu-list-card">
        <div class="card-header">
          <div>
            <h3>菜单列表</h3>
            <p>共 {{ menus.length }} 个配置项</p>
          </div>
        </div>

        <div class="menu-list">
          <button
            v-for="menu in menus"
            :key="menu.menuId"
            type="button"
            class="menu-item"
            :class="{ 'is-active': selectedMenuId === menu.menuId }"
            @click="selectMenu(menu.menuId)"
          >
            <div class="menu-item-top">
              <strong>{{ menu.menuName }}</strong>
              <ElTag size="small">{{ menu.icon }}</ElTag>
            </div>
            <span>{{ menu.path }}</span>
            <small>{{ menu.permissionCode }}</small>
          </button>
        </div>
      </article>

      <article class="menu-detail-card">
        <div class="card-header">
          <div>
            <h3>菜单详情</h3>
            <p v-if="selectedMenu">ID {{ selectedMenu.menuId }}</p>
          </div>
          <ElButton type="primary" :disabled="!selectedMenu" @click="selectedMenu && openEditDialog(selectedMenu)">
            编辑菜单
          </ElButton>
        </div>

        <ElSkeleton v-if="detailLoading" :rows="6" animated />
        <template v-else-if="selectedMenu">
          <div class="detail-grid">
            <article>
              <strong>菜单名称</strong>
              <span>{{ selectedMenu.menuName }}</span>
            </article>
            <article>
              <strong>路由路径</strong>
              <span>{{ selectedMenu.path }}</span>
            </article>
            <article>
              <strong>图标</strong>
              <span>{{ selectedMenu.icon }}</span>
            </article>
            <article>
              <strong>权限编码</strong>
              <span>{{ selectedMenu.permissionCode }}</span>
            </article>
          </div>

          <div class="preview-block">
            <h4>预览</h4>
            <div class="preview-card">
              <ElTag>{{ selectedMenu.icon }}</ElTag>
              <div>
                <strong>{{ selectedMenu.menuName }}</strong>
                <span>{{ selectedMenu.path }}</span>
              </div>
            </div>
          </div>

          <div class="options-grid">
            <section class="option-panel">
              <h4>可选图标</h4>
              <div class="chip-list">
                <ElTag v-for="icon in icons" :key="icon" :type="icon === selectedMenu.icon ? 'primary' : 'info'">
                  {{ icon }}
                </ElTag>
              </div>
            </section>

            <section class="option-panel">
              <h4>可选权限编码</h4>
              <div class="chip-list">
                <ElTag
                  v-for="permission in permissionOptions"
                  :key="permission.permissionId"
                  :type="permission.permissionCode === selectedMenu.permissionCode ? 'success' : 'info'"
                >
                  {{ permission.permissionCode }}
                </ElTag>
              </div>
            </section>
          </div>
        </template>
      </article>
    </section>

    <ElDialog v-model="saveDialogVisible" title="编辑菜单配置" width="520px">
      <ElForm ref="saveFormRef" :model="saveForm" :rules="saveRules" label-position="top">
        <ElFormItem label="菜单名称" prop="menuName">
          <ElInput v-model="saveForm.menuName" placeholder="请输入菜单名称" />
        </ElFormItem>
        <ElFormItem label="路由路径" prop="path">
          <ElInput v-model="saveForm.path" placeholder="请输入路由路径" />
        </ElFormItem>
        <ElFormItem label="图标" prop="icon">
          <ElSelect v-model="saveForm.icon" filterable placeholder="请选择内置图标">
            <ElOption v-for="icon in icons" :key="icon" :label="icon" :value="icon" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="权限编码" prop="permissionCode">
          <ElSelect v-model="saveForm.permissionCode" filterable placeholder="请选择权限编码">
            <ElOption
              v-for="permission in permissionOptions"
              :key="permission.permissionId"
              :label="`${permission.permissionName} / ${permission.permissionCode}`"
              :value="permission.permissionCode"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="saveDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="submitSave">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.menu-config-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.summary-card,
.menu-list-card,
.menu-detail-card {
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.08);
}

.hero-card h2 {
  margin: 10px 0 12px;
  font-size: 30px;
}

.hero-card p,
.summary-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: grid;
  gap: 10px;
}

.summary-card span {
  color: #64748b;
}

.summary-card strong {
  font-size: 28px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
}

.card-header,
.menu-item-top,
.preview-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.card-header {
  margin-bottom: 18px;
}

.card-header h3 {
  margin: 0;
}

.card-header p,
.menu-item span,
.menu-item small,
.detail-grid span,
.preview-card span {
  color: #64748b;
}

.menu-list {
  display: grid;
  gap: 12px;
}

.menu-item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid transparent;
  border-radius: 18px;
  text-align: left;
  cursor: pointer;
  background: #f8fafc;
}

.menu-item.is-active {
  border-color: rgba(75, 131, 205, 0.28);
  background: rgba(224, 233, 246, 0.78);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid article,
.option-panel,
.preview-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.detail-grid article {
  display: grid;
  gap: 6px;
}

.preview-block,
.options-grid {
  margin-top: 24px;
}

.preview-block h4,
.option-panel h4 {
  margin: 0 0 12px;
}

.preview-card {
  justify-content: flex-start;
}

.preview-card div {
  display: grid;
  gap: 6px;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 1100px) {
  .summary-grid,
  .workspace-grid,
  .detail-grid,
  .options-grid {
    grid-template-columns: 1fr;
  }
}
</style>



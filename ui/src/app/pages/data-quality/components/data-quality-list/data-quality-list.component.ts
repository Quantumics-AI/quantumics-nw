import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { EditRuleComponent } from '../edit-rule/edit-rule.component';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { ViewRunningRulesComponent } from '../view-running-rules/view-running-rules.component';
import { RuleCreationService } from '../../services/rule-creation.service';
import { RuleFilter } from '../../models/rule-filter.model';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;
  public loading:boolean;
  public ruleFilter = [
    { label: 'Inactive', name: 'Inactive', selected: false },
    { label: 'Deleted', name: 'Deleted', selected: false }
  ] as RuleFilter[];

  public dataQualityList: any;
  public ruleCount: any;
  public ruleCountInactive: any;
  public ruleCountDeleted: any;
  public projectId: number;
  userId: number;
  selectAllChecked: boolean = false;
  anyCheckboxSelected: boolean = false;

  // pagination
  public startIndex: number = 0;
  public pageSize: number = 10;
  public endIndex: number = this.pageSize;
  public page = 1;
  public rulesData: any;
  public paginationData: any;

  public ruleStatus: string = 'Active';
  public pageNumebr: number = 1;
  public pageLength: number = 10;

  public ruleId: any = [];

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private snakbar: SnackbarService,
    private ruleCreationService: RuleCreationService) {
    this.projectId = +this.activatedRoute.snapshot.paramMap.get('projectId');
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    this.getRules();
    this.getRulesInactive();
    this.getRulesDelete();
    sessionStorage.clear();
  }

  public getRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
    
      this.loading = false;
      this.dataQualityList = response?.result?.content;
      this.ruleCount =  response?.result?.content;
      this.paginationData = response?.result;

      if (this.dataQualityList.length > 1) {
        this.dataQualityList.sort((val1, val2) => {
          return (
            (new Date(val2.createdDate) as any) -
            (new Date(val1.createdDate) as any)
          );
        });
      }
      
    }, (error) => {
      this.loading = false;
    });
  }

  public getRulesInactive(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, 'Inactive', this.pageNumebr, this.pageLength).subscribe((response) => {
    
      this.ruleCountInactive =  response?.result?.content;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public getRulesDelete(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId,'Deleted', this.pageNumebr, this.pageLength).subscribe((response) => {
      
      this.loading = false;
      this.ruleCountDeleted =  response?.result?.content;
      
    }, (error) => {
      this.loading = false;
    });
  }

  public edit(dataQuality: any): void {

    this.router.navigate([`projects/${this.projectId}/data-quality/edit`],{
      queryParams: {
        dataId: dataQuality.ruleId
      }
    });
  }

  public viewRunning(): void {
    this.router.navigate([`projects/${this.projectId}/data-quality/view-rules`]);
    // const modalRef = this.modalService.open(ViewRunningRulesComponent, { size: 'lg', windowClass: 'modal-size', scrollable: false });
    // modalRef.componentInstance.userId = this.userId;
    // modalRef.componentInstance.projectId = this.projectId;


    // modalRef.result.then((result) => {
      
    // }, (result) => {
      
    // });
  }

  selectRuleAll(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.selectAllChecked = isChecked;
    this.anyCheckboxSelected = isChecked;
    // Loop through your dataQualityList and update the selected state of each item
    // this.dataQualityList.map(dataQuality => {
    //   dataQuality.selected = isChecked;
    //   if (dataQuality.selected) {
    //     this.ruleId.push(dataQuality.ruleId);
    //   } else {
        
    //   }
    // });

    if (isChecked) {
      // If "Select All" is checked, add all ruleIds to the ruleIds array
      this.ruleId = this.dataQualityList.map(dataQuality => dataQuality.ruleId);
    } else {
      // If "Select All" is unchecked, clear the ruleIds array
      this.ruleId = [];
    }
  
    // Update the selected state of each item in the dataQualityList
    this.dataQualityList.forEach(dataQuality => {
      dataQuality.selected = isChecked;
    });
  }
  

  selectRule(evt: Event, dataQuality: DataQuailtyListResponse): void {
    dataQuality.selected = (evt.target as HTMLInputElement).checked;
    if (dataQuality.selected) {
      // Add the ruleId to the ruleIds array if it's selected
      this.ruleId.push(dataQuality.ruleId);
    } else {
      // Remove the ruleId from the ruleIds array if it's deselected
    const index = this.ruleId.indexOf(dataQuality.ruleId);
    if (index !== -1) {
      this.ruleId.splice(index, 1);
    }
    }
    // Check if any checkbox is selected
    this.anyCheckboxSelected = this.dataQualityList.some(item => item.selected);
    // Check if all checkboxes are selected and update the "Select All" checkbox accordingly
    this.selectAllChecked = this.dataQualityList.every(item => item.selected);
    
  }

  public runRule(): void {
    this.dataQualityList.map(dataQuality => {
      dataQuality.selected = false;
    });
    this.anyCheckboxSelected = false;
    this.selectAllChecked = false;
    this.loading = true;
    const req = {
      ruleIds : this.ruleId
    }
    this.ruleCreationService.runRule(this.userId, this.projectId, req).subscribe((response) => {
      this.loading = false;
      this.ruleId = [];
      this.snakbar.open(response.message);
      
    }, (error) => {
      this.loading = false;
    });
  }

  public changeFilter(event: any, selectedFilter: RuleFilter): void {

    this.ruleFilter.forEach(filter => {
      if (filter !== selectedFilter) {
        filter.selected = false;
      }
    });
    selectedFilter.selected = event.target.checked;

    // Set the selected value as the name property or to null if both checkboxes are not selected
    if (this.ruleFilter.every(filter => !filter.selected)) {
      this.ruleStatus = 'Active';
    } else {
      this.ruleStatus = selectedFilter.name;
    }
    this.getStatusRules();
  }

  public getStatusRules(): void {
    this.loading = true;
    this.ruleCreationService.getRulesData(this.userId, this.projectId, this.ruleStatus, this.pageNumebr, this.pageLength).subscribe((response) => {
      
      this.dataQualityList = response?.result?.content;
      this.paginationData = response?.result;
      this.loading = false;
    }, (error) => {
      this.loading = false;
    });
  }
  

  // pagination 
  public onPageChange(currentPage: number): void {
    debugger
    if(this.pageNumebr < currentPage){
      this.pageNumebr = currentPage;
    } else {
      this.pageNumebr = this.pageNumebr - currentPage;
    }
    this.getRules();
    // this.pageNumebr = 
    this.startIndex = (currentPage - 1) * this.pageSize;
    this.endIndex = this.startIndex + this.pageSize;
  }
}
